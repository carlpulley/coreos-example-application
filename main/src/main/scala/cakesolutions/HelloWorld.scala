package cakesolutions

import akka.event.LoggingReceive
import akka.persistence.{SnapshotOffer, PersistentActor}
import cakesolutions.logging.Logging
import java.net.InetAddress

object HelloWorld {
  case object Ping
  case class Pong(message: String)
}

class HelloWorld extends PersistentActor with Logging with AutoPassivation {

  import HelloWorld._

  override val persistenceId = s"${getClass()}-${self.path.name}"

  def receiveRecover: Receive = {
    case SnapshotOffer =>
      // Intentionally do nothing
  }

  def receiveCommand = LoggingReceive {
    case Ping =>
      persist(Ping) { evt =>
        saveSnapshot(evt)
        sender() ! Pong(InetAddress.getLocalHost.getHostAddress)
      }
  }

}
