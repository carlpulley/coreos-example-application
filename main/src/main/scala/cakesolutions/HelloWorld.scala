package cakesolutions

import akka.actor.Props
import akka.contrib.pattern.ShardRegion
import akka.event.LoggingReceive
import akka.persistence.{SnapshotOffer, PersistentActor}
import cakesolutions.logging.Logging
import java.net.InetAddress

object HelloWorld {
  case object Ping
  case class Pong(message: String)

  val shardName = "hello-world"

  val idExtractor: ShardRegion.IdExtractor = {
    case Ping => ("HelloWorld", Ping)
  }

  val shardResolver: ShardRegion.ShardResolver = {
    case Ping => "1"
  }

  val props = Props[HelloWorld]
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
      case Ping =>
        persist(Ping) { evt =>
          saveSnapshot(evt)
          sender() ! Pong(InetAddress.getLocalHost.getHostAddress)
        }
    }
  }

}
