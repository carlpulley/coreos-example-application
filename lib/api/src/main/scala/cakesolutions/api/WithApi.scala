package cakesolutions

package api

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import spray.can.Http
import spray.routing._

import scala.concurrent.ExecutionContext

trait WithApi {
  this: Configuration { val system: ActorSystem } =>

  def startupApi(api: ExecutionContext => Route): Unit = {
    val route: Route = api(system.dispatcher)
    val rootService = system.actorOf(Props(new RootService(route)), "api")
    IO(Http)(system).tell(Http.Bind(rootService, interface = config.getString("application.hostname"), port = config.getInt("application.port")), rootService)
  }

}
