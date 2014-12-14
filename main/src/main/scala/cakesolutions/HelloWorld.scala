package cakesolutions

import akka.actor.Props
import akka.contrib.pattern.ShardRegion
import akka.event.LoggingReceive
import akka.persistence.{SnapshotOffer, PersistentActor}
import cakesolutions.logging.Logging
import com.typesafe.config.ConfigFactory
import scala.collection.JavaConversions._
import scala.language.postfixOps

object HelloWorld extends Configuration {
  case class Ping(index: Int)
  case class Pong(message: String)

  val shardName = "hello-world"

  val idExtractor: ShardRegion.IdExtractor = {
    case ping @ Ping(index) => (s"HelloWorld-$index", ping)
  }

  val shardResolver: ShardRegion.ShardResolver = {
    case Ping(index) => (index % config.getInt("application.shards")).toString
  }

  val props = Props[HelloWorld]

  def shardProps: Option[Props] = {
    val roles = ConfigFactory.load().getStringList("akka.cluster.roles")
    roles.find(shardName ==).map(_ => props)
  }
}

class HelloWorld extends PersistentActor with Configuration with Logging with AutoPassivation {

  import HelloWorld._

  val persistenceId: String = s"${getClass()}-${self.path.name}"

  def receiveRecover: Receive = LoggingReceive {
    case SnapshotOffer =>
      // Intentionally do nothing
  }

  def receiveCommand: Receive = LoggingReceive {
    withPassivation {
      case ping: Ping =>
        persist(ping) { evt =>
          saveSnapshot(evt)
          sender() ! Pong(config.getString("application.hostname"))
        }
    }
  }

}
