package cakesolutions

import akka.actor.ActorDSL._
import akka.actor.ActorSystem
import akka.event.LoggingReceive
import cakesolutions.logging.Logging

object ActorExample extends App {
  implicit val system = ActorSystem()

  val act = actor(new Act with Logging {
    override def receive = LoggingReceive {
      case msg @ "event" =>
        log.info(s"Received $msg")

      case msg @ "exception" =>
        log.error(s"Exception $msg")

      case msg @ "shutdown" =>
        log.debug("Shutdown")
        system.shutdown()
    }
  })

  act ! "event"
  act ! "exception"
  act ! "shutdown"
}
