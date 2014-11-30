package cakesolutions

import akka.actor.{Cancellable, AddressFromURIString, Address, ActorSystem}
import akka.cluster.{Cluster, MemberStatus}
import akka.kernel.Bootable
import cakesolutions.logging.Logger
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import net.nikore.etcd.EtcdJsonProtocol.EtcdListResponse
import scala.concurrent.duration._
import scala.util.{Failure, Success}

abstract class BootableCluster(val system: ActorSystem) extends Bootable with ExceptionLogging {
  this: etcd.Configuration =>

  val hostname = InetAddress.getLocalHost.getHostName
  val cluster = Cluster(system)
  val log = Logger(this.getClass)
  val clusterNodes = config.getString("etcd.akka")
  val keyTimeout = config.getDuration("etcd.key.timeout", TimeUnit.SECONDS).seconds
  val keyRefresh = config.getDuration("etcd.key.refresh", TimeUnit.SECONDS).seconds
  val failureThreshold = config.getInt("etcd.key.failure.count")
  val retry = config.getDuration("akka.cluster.retry", TimeUnit.SECONDS).seconds

  var updateKey: Option[Cancellable] = None

  import system.dispatcher

  // Register cluster MemberUp callback
  cluster.registerOnMemberUp {
    etcd.setKey(s"$clusterNodes/${clusterAddressKey()}", MemberStatus.Up.toString, ttl = Some(keyTimeout))
  }
  // Register shutdown callback
  system.registerOnTermination(shutdown())

  def updateClusterAddressKey(failureCount: Int = 0): Cancellable = system.scheduler.scheduleOnce(keyRefresh) {
    val status = cluster.state.members.find(_.address == cluster.selfAddress).map(_.status)
    if (status.isDefined) {
      etcd.setKey(s"$clusterNodes/${clusterAddressKey()}", status.get.toString, ttl = Some(keyTimeout)).onComplete {
        case Success(_) =>
          updateKey = Some(updateClusterAddressKey())

        case Failure(exn) if failureCount >= failureThreshold =>
          log.error(s"Final try at attempting to update the etcd key: $clusterNodes/${clusterAddressKey()} - ${exceptionString(exn)}")
          etcd.deleteKey(s"$clusterNodes/${clusterAddressKey()}").onSuccess {
            case _ =>
              log.info(s"Deleted key $clusterNodes/${clusterAddressKey()}")
          }
          updateKey = None

        case Failure(exn) =>
          log.error(s"Failed to update the etcd key on try ${failureCount + 1}: $clusterNodes/${clusterAddressKey()} - ${exceptionString(exn)}")
          updateKey = Some(updateClusterAddressKey(failureCount + 1))
      }
    } else {
      log.error("Cluster currently does not have a state recorded for this node - shutting down!")
      shutdown()
    }
  }

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
                .map(n => clusterAddress(n.key.stripSuffix(s"/$clusterNodes/")))

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
    etcd.setKey(s"$clusterNodes/${clusterAddressKey()}", MemberStatus.Joining.toString, ttl = Some(keyTimeout)).onComplete {
      case Success(_) =>
        // Ensure key TTLs are maintained
        updateKey = Some(updateClusterAddressKey())
        // Now we retrieve seed nodes and join the collective
        joinCluster()

      case Failure(exn) =>
        log.error(s"Failed to set state to '${MemberStatus.Joining}' with etcd: $exn")
        shutdown()
    }
  }

  def shutdown(): Unit = {
    // We first ensure that we de-register and leave the cluster!
    etcd.deleteKey(s"$clusterNodes/${clusterAddressKey()}")
    updateKey.map(_.cancel())
    cluster.leave(cluster.selfAddress)
    system.shutdown()
  }

}
