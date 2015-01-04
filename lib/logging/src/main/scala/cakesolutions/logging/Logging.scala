package cakesolutions

package logging

import akka.actor.Actor
import akka.actor.ActorLogging

/**
 * Trait to add specialised logging to Actors
 */
trait Logging extends Actor with ActorLogging with ExceptionLogging {

  /**
   * We log when the actor has started
   */
  override def preStart() = {
    log.info(s"${getClass.getSimpleName} started")
    super.preStart()
  }

  /**
   * We log when the actor restarts
   *
   * @param cause the exception that has caused this restart
   * @param msg   (optional) the message that this actor was processing when it restarted
   */
  override def preRestart(cause: Throwable, msg: Option[Any]) = {
    log.info(s"${getClass.getSimpleName} restarting whilst handling ${msg.map(_.getClass.getSimpleName)} - reason: ${exceptionString(cause)}")
    log.debug(s"${getClass.getSimpleName} restarting whilst handling $msg - reason: ${exceptionString(cause)}")
    super.preRestart(cause, msg)
  }

  /**
   * We log when the actor stops
   */
  override def postStop() = {
    log.info(s"${getClass.getSimpleName} stopping")
    super.postStop()
  }

  /**
   * We ensure that receive functions have message receiving logged at DEBUG level
   */
  override def aroundReceive(receive: Receive, msg: Any): Unit = {
    log.debug(s"${getClass.getSimpleName} received message $msg from actor ${sender()}")
    super.aroundReceive(receive, msg)
  }

}
