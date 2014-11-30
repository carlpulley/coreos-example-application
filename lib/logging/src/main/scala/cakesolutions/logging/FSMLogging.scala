package cakesolutions

package logging

import akka.actor.LoggingFSM

/**
 * Trait to add specialised logging into FSM based actors
 */
trait FSMLogging[State, Data] extends LoggingFSM[State, Data] with Logging {
  this: Configuration =>

  // By default (i.e. in production etc) this value should be zero
  override def logDepth = if (log.isDebugEnabled) config.getInt("akka.actor.debug.logging.depth") else 0

  /**
   * For FSM actors, we also dump our trace buffers when restarted.
   *
   * @param cause the exception that has caused this restart
   * @param msg   (optional) the message that this actor was processing when it restarted
   */
  override def preRestart(cause: Throwable, msg: Option[Any]) = {
    log.debug(s"Restarting ${getClass.getSimpleName}: $logDepth events from logging trace buffers: $getLog")
    super.preRestart(cause, msg)
  }

  /**
   * For FSM actors, we also dump our trace buffers when stopped.
   */
  override def postStop() = {
    log.debug(s"Stopping ${getClass.getSimpleName}: $logDepth events from logging trace buffers: $getLog")
    super.postStop()
  }

}
