package cakesolutions

import java.net.InetAddress

import akka.actor.{ActorSystem, Address, AddressFromURIString, Props}
import akka.cluster.Cluster
import akka.io.IO
import akka.kernel.Bootable
import cakesolutions.api.RootService
import cakesolutions.logging.Logger
import java.util.concurrent.TimeUnit
import net.nikore.etcd.EtcdJsonProtocol.EtcdListResponse
import spray.can.Http
import scala.concurrent.duration._
import scala.util._

// TODO: refactor service discovery code into a separate etcd client actor

class Main extends Bootable with Configuration with ExceptionLogging {

  import EtcdKeys._

  val hostname = InetAddress.getLocalHost().getHostName()
  val name = config.getString("akka.system")
  val system = ActorSystem(name)
  val cluster = Cluster(system)
  val log = Logger(this.getClass)
  val retry = config.getDuration("akka.cluster.retry", TimeUnit.SECONDS).seconds

  import system.dispatcher

  cluster.registerOnMemberUp {
    etcd.setKey(s"$ClusterNodes/$hostname.state", "MemberUp")
  }

  /**
   * Used to retrieve (from etcd) potential seed nodes for forming our Akka cluster and to then build our cluster.
   */
  def joinCluster(): Unit = {
    if (cluster.selfRoles.contains("initial-seed")) {
      // We are an initial seed node, so we only need to join our own cluster
      cluster.join(cluster.selfAddress)
    } else {
      // We are not an initial seed node, so we need to fetch up cluster nodes for seeding
      etcd.listDir(ClusterNodes, recursive = true).onComplete {
        case Success(response: EtcdListResponse) =>
          log.debug(s"Using etcd response: $response")
          response.node.nodes match {
            // We are only interested in actor systems which have registered and recorded themselves as up
            case Some(systemNodes)
              if systemNodes.filter(_.key.endsWith(".state")).filterNot(_.key == s"/$ClusterNodes/$hostname.state").filter(_.value == Some("MemberUp")).nonEmpty => {

              // At least one actor system address has been retrieved from etcd - we now need to check their respective etcd states and locate cluster seed nodes
              val nodes: Map[String, Address] =
                systemNodes
                  .filter(_.key.endsWith(".address"))
                  .filterNot(_.key == s"/$ClusterNodes/$hostname.address")
                  .flatMap(n => n.value.map(addr => (n.key.stripSuffix(".address"), AddressFromURIString(addr))))
                  .toMap
              val upNodes: List[String] =
                systemNodes
                  .filter(_.key.endsWith(".state"))
                  .filterNot(_.key == s"/$ClusterNodes/$hostname.state")
                  .filter(_.value == Some("MemberUp"))
                  .map(_.key.stripSuffix(".state"))

              if (nodes.filterKeys(upNodes.contains).nonEmpty) {
                val seedNodes = nodes.filterKeys(upNodes.contains).values.toList
                log.info(s"Joining our cluster using the seed nodes: $seedNodes")
                cluster.joinSeedNodes(seedNodes)
              } else {
                log.info(s"Failed to retrieve any viable seed nodes - retrying in $retry seconds")
                system.scheduler.scheduleOnce(retry)(joinCluster())
              }
            }

            case Some(_) =>
              log.error(s"Failed to retrieve any system addresses - retrying in $retry seconds")
              system.scheduler.scheduleOnce(retry)(joinCluster())

            case None =>
              log.error(s"Failed to retrieve any keys for directory $ClusterNodes - retrying in $retry seconds")
              system.scheduler.scheduleOnce(retry)(joinCluster())
          }

        case Failure(exn) =>
          log.error(s"Failed to contact etcd: ${exceptionString(exn)}")
          shutdown()
      }
    }
  }

  def startup(): Unit = {
    // We first setup basic cluster registration information
    etcd.setKey(s"$ClusterNodes/$hostname.address", cluster.selfAddress.toString)
    // Now we retrieve seed nodes and join the collective
    joinCluster()
    val applicationActor = system.actorOf(Props[HelloWorld])
    val rootService = system.actorOf(Props(new RootService(applicationActor)), "api")
    IO(Http)(system).tell(Http.Bind(rootService, interface = config.getString("application.hostname"), port = config.getInt("application.port")), rootService)
    // Should we run the DEBUG console?
    if (config.getBoolean("application.debug")) {
      debug.Console()
    }
  }

  def shutdown(): Unit = {
    // We first ensure that we de-register and leave the cluster!
    etcd.deleteKey(s"$ClusterNodes/$hostname")
    cluster.leave(cluster.selfAddress)
    system.shutdown()
  }

}
