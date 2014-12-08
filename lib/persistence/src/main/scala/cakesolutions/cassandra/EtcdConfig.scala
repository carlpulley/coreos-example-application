package cakesolutions

package cassandra

import akka.actor.{Stash, ActorRef, Props}
import akka.event.LoggingReceive
import cakesolutions.etcd.WithEtcd
import cakesolutions.logging.{Logging => LoggingActor}
import com.typesafe.config.{Config, ConfigFactory}
import java.util.concurrent.TimeUnit
import net.nikore.etcd.EtcdExceptions.KeyNotFoundException
import net.nikore.etcd.EtcdJsonProtocol.{EtcdListResponse, NodeListElement}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class EtcdConfig(props: Config => Props, key: String) extends LoggingActor with Stash {
  this: Configuration with WithEtcd =>

  import context.dispatcher

  val cassandraKey = config.getString("cassandra.etcd.key")
  val retry = config.getDuration("cassandra.etcd.retry", TimeUnit.SECONDS).seconds
  var store: Option[ActorRef] = None

  // Until Cassandra contact points have been defined, all received messages are stashed
  def receive = {
    case msg =>
      log.warning(s"Cassandra is not currently up - stashing: $msg")
      stash()
  }

  // Once Cassandra contact points have been defined, all messages get forwarded to the plugin actor
  // FIXME: do we need to handle the case whereby store.isEmpty???
  def forward: Receive = LoggingReceive {
    case msg if store.isDefined =>
      store.foreach(_.tell(msg, sender()))
  }

  /**
   * Used to retrieve Cassandra contact points from an etcd based service discovery.
   */
  def getContactPoints(): Unit = {
    context.system.scheduler.scheduleOnce(retry) {
      etcd.listDir(s"/$cassandraKey", recursive = false).onComplete {
        case Success(EtcdListResponse(_, NodeListElement(_, _, _, Some(nodes)))) if nodes.map(_.key).nonEmpty =>
          val contactPoints = nodes.map(_.key.stripPrefix(s"/$cassandraKey/"))
          log.info(s"Cassandra contact points taken to be $contactPoints")
          // Define Cassandra storage actor using new contact points - we override any such application.conf definition here
          val updatedConfig = ConfigFactory.parseString(contactPoints.mkString("contact-points=[ \"", "\", \"", "\" ]")).withFallback(config.getConfig(key))
          store = Some(context.actorOf(props(updatedConfig)))
          // Change actor behaviour to enable message forwarding to the Cassandra storage plugin
          unstashAll()
          context.become(forward)

        case Failure(KeyNotFoundException("Key not found", "not found", _)) =>
          log.warning(s"etcd key '$cassandraKey' does not exist - retrying in $retry seconds")
          getContactPoints()

        case Failure(exn) =>
          log.error(s"Failed to contact etcd: ${exceptionString(exn)} - shutting down!")
          context.system.shutdown()

        case _ =>
          log.warning(s"Failed to retrieve any Cassandra contact points from /$cassandraKey - retrying in $retry seconds")
          getContactPoints()
      }
    }
  }

  getContactPoints()

}
