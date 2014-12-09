package cakesolutions

package api

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import java.util.concurrent.TimeUnit._
import scala.concurrent.ExecutionContext
import spray.routing._

trait Service extends Directives with Configuration with ExceptionLogging {

  import HelloWorld._

  implicit val timeout: Timeout = Timeout(config.getDuration("application.timeout", SECONDS), SECONDS)

  // actorRef here may be a shard region, so we need to lazily evaluate this actor reference for each message that's routed
  def applicationRoute(actorRef: => ActorRef)(implicit ec: ExecutionContext): Route = {
    path("ping") {
      get {
        complete {
          (actorRef ? Ping).mapTo[Pong].map(_.message)
        }
      }
    }
  }

}
