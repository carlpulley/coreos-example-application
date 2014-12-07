package cakesolutions

import akka.actor.{Actor, ActorLogging, ReceiveTimeout}
import akka.event.LoggingReceive

trait AutoPassivation extends ActorLogging {
  this: Actor =>

  import akka.contrib.pattern.ShardRegion.Passivate

  private val passivationReceive: Receive = LoggingReceive {
    case ReceiveTimeout =>
      context.parent ! Passivate(stopMessage = 'stop)

    case 'stop =>
      context.stop(self)
  }

  protected def withPassivation(receive: Receive): Receive = receive.orElse(passivationReceive)

}
