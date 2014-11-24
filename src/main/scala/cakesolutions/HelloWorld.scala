package cakesolutions

import akka.event.LoggingReceive
import cakesolutions.logging.Logging

object HelloWorld {
  case object Ping
  case class Pong(message: String)
}

class HelloWorld extends Logging {

  import HelloWorld._

  def receive = LoggingReceive {
    case Ping =>
      sender() ! Pong(context.system.settings.name)
  }

}
