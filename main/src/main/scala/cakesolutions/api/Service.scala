package cakesolutions

package api

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import spray.routing._

trait Service extends Directives with Configuration with ExceptionLogging {

  import HelloWorld._

  implicit val timeout: Timeout = Timeout(config.getDuration("application.timeout", SECONDS).seconds)

  def applicationRoute(actorRef: ActorRef)(implicit ec: ExecutionContext) = {
    path("ping") {
      get {
        complete {
          (actorRef ? Ping).mapTo[Pong].map(_.message)
        }
      }
    }
  }

}
