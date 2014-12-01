package cakesolutions

import akka.actor._
import akka.cluster.ClusterEvent._
import akka.cluster.{Cluster, MemberStatus}
import akka.kernel.Bootable
import cakesolutions.etcd.ClusterMonitor
import cakesolutions.logging.Logger
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import net.nikore.etcd.EtcdJsonProtocol.EtcdListResponse
import scala.concurrent.duration._
import scala.util.{Failure, Success}

abstract class BootableCluster(val system: ActorSystem) extends Bootable with Configuration with ExceptionLogging {

  val hostname = InetAddress.getLocalHost.getHostName
  val cluster = Cluster(system)
  val log = Logger(this.getClass)
  val clusterNodes = config.getString("etcd.akka")
  val retry = config.getDuration("akka.cluster.retry", TimeUnit.SECONDS).seconds

  import system.dispatcher

  // Register cluster MemberUp callback
  cluster.registerOnMemberUp {
    etcd.setKey(s"$clusterNodes/${clusterAddressKey()}", MemberStatus.Up.toString)
    // Subscribe to cluster membership events and maintain etcd key state
    val monitor = system.actorOf(Props(new ClusterMonitor(s"$clusterNodes/${clusterAddressKey()}")))
    cluster.subscribe(monitor, classOf[UnreachableMember], classOf[MemberRemoved], classOf[MemberExited], classOf[MemberUp])
  }
  // Register shutdown callback
  system.registerOnTermination(shutdown())

  def clusterAddressKey(): String = {
    s"${cluster.selfAddress.host.getOrElse("")}:${cluster.selfAddress.port.getOrElse(0)}"
  }

  def clusterAddress(key: String): Address = {
    AddressFromURIString(s"akka.tcp://${config.getString("akka.system")}@$key")
  }

  /**
   * Used to retrieve (from etcd) potential seed nodes for forming our Akka cluster and to then build our cluster.
   */
  def joinCluster(): Unit = {
    // We are not an initial seed node, so we need to fetch up cluster nodes for seeding
    etcd.listDir(clusterNodes, recursive = false).onComplete {
      case Success(response: EtcdListResponse) =>
        log.debug(s"Using etcd response: $response")
        response.node.nodes match {
          // We are only interested in actor systems which have registered and recorded themselves as up
          case Some(systemNodes)
            if systemNodes.filter(_.value == Some(MemberStatus.Up.toString)).nonEmpty => {

            // At least one actor system address has been retrieved from etcd - we now need to check their respective etcd states and locate cluster seed nodes
            val seedNodes =
              systemNodes
                .filter(_.value == Some(MemberStatus.Up.toString))
                .map(n => clusterAddress(n.key.stripPrefix(s"/$clusterNodes/")))

            log.info(s"Joining the cluster using the seed nodes: $seedNodes")
            cluster.joinSeedNodes(seedNodes)
          }

          case Some(_) =>
            log.error(s"Failed to retrieve any system addresses - retrying in $retry seconds")
            system.scheduler.scheduleOnce(retry)(joinCluster())

          case None =>
            log.error(s"Failed to retrieve any keys for directory $clusterNodes - retrying in $retry seconds")
            system.scheduler.scheduleOnce(retry)(joinCluster())
        }

      case Failure(exn) =>
        log.error(s"Failed to contact etcd: ${exceptionString(exn)}")
        shutdown()
    }
  }

  def startup(): Unit = {
    // We first setup basic cluster registration information
    etcd.setKey(s"$clusterNodes/${clusterAddressKey()}", MemberStatus.Joining.toString).onComplete {
      case Success(_) =>
        // Now we retrieve seed nodes and join the collective
        joinCluster()

      case Failure(exn) =>
        log.error(s"Failed to set state to '${MemberStatus.Joining}' with etcd: $exn")
        shutdown()
    }
  }

  def shutdown(): Unit = {
    // First ensure that we de-register our etcd key and then we leave the cluster!
    etcd.deleteKey(s"$clusterNodes/${clusterAddressKey()}").onComplete {
      case _ =>
        cluster.leave(cluster.selfAddress)
        system.shutdown()
    }
  }

}
