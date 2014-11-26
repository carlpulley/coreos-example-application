package cakesolutions

import java.net.InetAddress

import akka.actor.{ActorSystem, AddressFromURIString, Props}
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
  val name = s"${config.getString("akka.system")}-$hostname"
  val system = ActorSystem(name)
  val cluster = Cluster(system)
  val log = Logger(this.getClass)
  val retry = config.getDuration("akka.cluster.retry", TimeUnit.SECONDS).seconds

  import system.dispatcher

  /**
   * Used to retrieve (from etcd) potential seed nodes for forming our Akka cluster
   */
  def joinCluster(): Unit = {
    etcd.listDir(ClusterNodes).onComplete {
      case Success(response: EtcdListResponse) =>
        log.debug(s"Using etcd response: $response")
        response.node.nodes match {
          case Some(seedNodes) if (seedNodes.filterNot(_.key == s"/$ClusterNodes/$hostname").flatMap(_.value).nonEmpty) =>
            // At least one seed node has been retrieved from etcd
            val nodes = Random.shuffle(seedNodes.filterNot(_.key == s"/$ClusterNodes/$hostname").flatMap(_.value).map(AddressFromURIString.apply))
            log.info(s"Seeding cluster using: $nodes")
            cluster.joinSeedNodes(nodes)

          case Some(_) =>
            log.error(s"Failed to retrieve any viable seed nodes - retrying in $retry seconds")
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

  def startup(): Unit = {
    // We first ensure that we register with our cluster!
    etcd.setKey(s"$ClusterNodes/$hostname", cluster.selfAddress.toString)
    // Now retrieve seed nodes and join the collective
    joinCluster()
    val applicationActor = system.actorOf(Props[HelloWorld])
    val rootService = system.actorOf(Props(new RootService(applicationActor)), "api")
    IO(Http)(system).tell(Http.Bind(rootService, interface = config.getString("application.hostname"), port = config.getInt("application.port")), rootService)
  }

  def shutdown(): Unit = {
    // We first ensure that we de-register and leave the cluster!
    etcd.deleteKey(s"$ClusterNodes/$hostname")
    cluster.leave(cluster.selfAddress)
    system.shutdown()
  }

}
