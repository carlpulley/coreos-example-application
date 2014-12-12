package cakesolutions

package api

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import spray.can.Http
import spray.routing._

trait WithApi {
  this: Configuration =>

  def startupApi(api: RestApi)(implicit system: ActorSystem): Unit = {
    system.log.debug("Starting REST API")

    api.route map {
      case api =>
        val route: Route = api(system.dispatcher)
        val restService = system.actorOf(Props(new RestService(route)), "api")
        IO(Http)(system) ! Http.Bind(restService, interface = config.getString("application.hostname"), port = config.getInt("application.port"))
        system.log.debug(s"Started REST API services using handler $restService")
    }

    api.start.map(_())
  }

  def shutdownApi(api: RestApi): Unit =
    api.stop.map(_())

}
