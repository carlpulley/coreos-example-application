package cakesolutions

import akka.actor.{Actor, ActorLogging, ReceiveTimeout}
import akka.contrib.pattern.ShardRegion.Passivate
import akka.event.LoggingReceive
import java.util.concurrent.TimeUnit
import scala.concurrent.duration._

trait AutoPassivation extends ActorLogging {
  this: Actor with Configuration =>

  context.setReceiveTimeout(config.getDuration("application.passivate", TimeUnit.SECONDS).seconds)

  private val passivationReceive: Receive = LoggingReceive {
    case ReceiveTimeout =>
      context.parent ! Passivate(stopMessage = 'stop)

    case 'stop =>
      context.stop(self)
  }

  protected def withPassivation(receive: Receive): Receive = receive.orElse(passivationReceive)

}
