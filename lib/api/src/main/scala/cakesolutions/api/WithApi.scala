package cakesolutions

package api

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import scala.concurrent.ExecutionContext
import spray.can.Http
import spray.routing._

trait WithApi {
  this: Configuration =>

  def startupApi(api: ExecutionContext => Route)(implicit system: ActorSystem): Unit = {
    val route: Route = api(system.dispatcher)
    val restService = system.actorOf(Props(new RestService(route)), "api")
    IO(Http)(system) ! Http.Bind(restService, interface = config.getString("application.hostname"), port = config.getInt("application.port"))
  }

}
