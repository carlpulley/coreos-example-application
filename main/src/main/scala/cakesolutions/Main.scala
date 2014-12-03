package cakesolutions

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import cakesolutions.api.RootService
import spray.can.Http

class Main extends BootableCluster(ActorSystem("HelloWorld")) with JoinConstraint with Configuration {

  override def startup(): Unit = {
    super.startup()
    val applicationActor = system.actorOf(Props[HelloWorld])
    val rootService = system.actorOf(Props(new RootService(applicationActor)), "api")
    IO(Http)(system).tell(Http.Bind(rootService, interface = config.getString("application.hostname"), port = config.getInt("application.port")), rootService)
  }

}
