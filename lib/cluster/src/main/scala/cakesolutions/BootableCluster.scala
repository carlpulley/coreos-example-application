package cakesolutions

import akka.actor._
import akka.cluster.ClusterEvent._
import akka.cluster.{Cluster, MemberStatus}
import akka.kernel.Bootable
import cakesolutions.etcd.{WithEtcd, ClusterMonitor}
import cakesolutions.logging.Logger
import java.util.concurrent.TimeUnit
import net.nikore.etcd.EtcdJsonProtocol.EtcdListResponse
import scala.concurrent.duration._
import scala.util.{Failure, Success}

abstract class BootableCluster(_system: ActorSystem) extends Bootable with ExceptionLogging {
  this: JoinConstraint with Configuration with WithEtcd =>

  implicit val system = _system
  val cluster = Cluster(system)
  val log = Logger(this.getClass)
  val nodeKey = config.getString("akka.etcd.key")
  val retry = config.getDuration("akka.cluster.retry", TimeUnit.SECONDS).seconds

  import system.dispatcher

  // Register cluster MemberUp callback
  cluster.registerOnMemberUp {
    log.debug("MemberUp callback triggered - recording Up status in etcd registry")
    etcd.setKey(s"$nodeKey/${clusterAddressKey()}", MemberStatus.Up.toString).onComplete {
      case Success(_) =>
        // Subscribe to cluster membership events and maintain etcd key state
        log.info(s"${clusterAddressKey()} marked as up - registering for cluster membership changes")
        val monitor = system.actorOf(Props(new ClusterMonitor(etcd, s"$nodeKey/${clusterAddressKey()}")))
        cluster.subscribe(monitor, classOf[UnreachableMember], classOf[MemberRemoved], classOf[MemberExited], classOf[MemberUp])

      case Failure(exn) =>
        log.error(s"Failed to set state to '${MemberStatus.Up.toString}' with etcd: ${exceptionString(exn)} - shutting down!")
        shutdown()
    }
  }
  // Register shutdown callback
  system.registerOnTermination(shutdown())

  def clusterAddressKey(): String = {
    s"${cluster.selfAddress.host.getOrElse("")}:${cluster.selfAddress.port.getOrElse(0)}"
  }

  def clusterAddress(key: String): Address = {
    AddressFromURIString(s"akka.tcp://${system.name}@$key")
  }

  /**
   * Used to retrieve (from etcd) potential seed nodes for forming our Akka cluster and to then build our cluster.
   */
  def joinCluster(): Unit = {
    // We are not an initial seed node, so we need to fetch up cluster nodes for seeding
    etcd.listDir(nodeKey, recursive = false).onComplete {
      case Success(response: EtcdListResponse) =>
        log.debug(s"Using etcd response: $response")

        response.node.nodes match {
          // Have any actor systems registered and recorded themselves as up?
          case Some(systemNodes)
            if systemNodes.filter(_.value == Some(MemberStatus.Up.toString)).nonEmpty => {

            // At least one actor system address has been retrieved from etcd - we now need to check their respective etcd states and locate cluster seed nodes
            val seedNodes =
              systemNodes
                .filter(_.value == Some(MemberStatus.Up.toString))
                .map(n => clusterAddress(n.key.stripPrefix(s"/$nodeKey/")))
            // Any node that is 'Up' or 'Joining' is considered to be a joining node
            val joiningNodes =
              systemNodes
                .filter(n => List(Some(MemberStatus.Up.toString), Some(MemberStatus.Joining.toString)).contains(n.value))

            joinConstraint(joiningNodes) {
              log.info(s"Building the cluster using the seed nodes: $seedNodes")
              cluster.joinSeedNodes(seedNodes)
            }
          }

          case Some(_) =>
            log.warning(s"Failed to retrieve any system addresses - retrying in $retry seconds")
            system.scheduler.scheduleOnce(retry)(joinCluster())

          case None =>
            log.warning(s"Failed to retrieve any keys for directory $nodeKey - retrying in $retry seconds")
            system.scheduler.scheduleOnce(retry)(joinCluster())
        }

      case Failure(exn) =>
        log.error(s"Failed to contact etcd: ${exceptionString(exn)} - shutting down!")
        shutdown()
    }
  }

  def startup(): Unit = {
    // We first setup basic cluster registration information
    etcd.setKey(s"$nodeKey/${clusterAddressKey()}", MemberStatus.Joining.toString).onComplete {
      case Success(_) =>
        // Now we retrieve seed nodes and join the collective
        joinCluster()

      case Failure(exn) =>
        log.error(s"Failed to set state to '${MemberStatus.Joining}' with etcd: $exn - shutting down!")
        shutdown()
    }
  }

  def shutdown(): Unit = {
    // First ensure that we de-register our etcd key and then we leave the cluster!
    etcd.deleteKey(s"$nodeKey/${clusterAddressKey()}").onComplete {
      case _ =>
        cluster.leave(cluster.selfAddress)
        system.shutdown()
    }
  }

}
