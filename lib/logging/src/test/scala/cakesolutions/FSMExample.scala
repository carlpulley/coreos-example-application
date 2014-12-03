package cakesolutions

import akka.actor.ActorDSL._
import akka.actor.ActorSystem
import cakesolutions.logging.FSMLogging

object FSMExample extends App {
  implicit val system = ActorSystem()

  val act = actor(new Act with logging.Configuration with FSMLogging[String, Int] {
    startWith("init", 0)

    when("init") {
      case msg @ Event("event", st) =>
        log.info(s"Received $msg")
        goto("next") using 1
    }

    when("next") {
      case msg @ Event("next", st) =>
        log.error(s"Received $msg")
        goto("final") using 2
    }

    when("final") {
      case msg @ Event("shutdown", st) =>
        log.debug(s"Received $msg")
        stop()
    }

    onTermination {
      case StopEvent(_, "final", _) =>
        system.shutdown()
    }

    initialize()
  })

  act ! "event"
  act ! "next"
  act ! "shutdown"
}
