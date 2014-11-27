package cakesolutions

import akka.event.LoggingReceive
import cakesolutions.logging.Logging
import java.net.InetAddress

object HelloWorld {
  case object Ping
  case class Pong(message: String)
}

class HelloWorld extends Logging {

  import HelloWorld._

  def receive = LoggingReceive {
    case Ping =>
      sender() ! Pong(InetAddress.getLocalHost.getHostAddress)
  }

}
