package cakesolutions.api

import akka.actor.{ActorRef, Address}

trait BootableService {

  def boot(address: Address, handlers: ActorRef*) = RestApi()

}
