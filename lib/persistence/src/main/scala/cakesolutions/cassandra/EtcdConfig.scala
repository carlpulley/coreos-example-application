package cakesolutions.cassandra

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.typesafe.config.{Config, ConfigFactory}
import java.util.concurrent.TimeUnit
import net.nikore.etcd.EtcdClient
import net.nikore.etcd.EtcdJsonProtocol.{EtcdListResponse, NodeListElement}
import scala.concurrent.duration._

class EtcdConfig(props: Config => Props, key: String) extends Actor with ActorLogging {

  import context.dispatcher

  val config = context.system.settings.config
  val etcd = new EtcdClient(config.getString("etcd.url"))
  val cassandraKey = config.getString("cassandra.etcd.key")
  val retry = config.getDuration("cassandra.etcd.retry", TimeUnit.SECONDS).seconds
  var store: Option[ActorRef] = None

  // Until Cassandra contact points have been defined, all messages are ignored - i.e. dead lettered
  // FIXME: should we be stashing these messages???
  def receive = Actor.emptyBehavior

  // Once Cassandra contact points have been defined, all messages get forwarded to the plugin actor
  def forward: Receive = {
    case msg if store.isDefined =>
      store.get.tell(msg, sender())
  }

  /**
   * Used to retrieve Cassandra contact points from an etcd based service discovery.
   */
  def getContactPoints(): Unit = {
    context.system.scheduler.scheduleOnce(retry) {
      etcd.listDir(s"/$cassandraKey", recursive = false).map {
        case EtcdListResponse(_, NodeListElement(_, _, _, Some(nodes))) if nodes.flatMap(_.value).nonEmpty =>
          val contactPoints = nodes.flatMap(_.value)
          log.info(s"Cassandra contact points taken to be $contactPoints")
          // Define Cassandra storage actor using new contact points - we override any such application.conf definition here
          val updatedConfig = ConfigFactory.parseString(s"contact-points=[${contactPoints.mkString("\"", "\", \"", "\"")}}]").withFallback(config.getConfig(key))
          store = Some(context.actorOf(props(updatedConfig)))
          // Change actor behaviour to enable message forwarding to the Cassandra storage plugin
          context.become(forward)

        case _ =>
          log.error(s"Failed to retrieve any Cassandra contact points from /$cassandraKey - retrying in $retry seconds")
          getContactPoints()
      }
    }
  }

  getContactPoints()

}
