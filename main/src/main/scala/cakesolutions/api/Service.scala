package cakesolutions

package api

import akka.actor.{ActorRef, Address}
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import spray.routing._

trait Service extends Directives with Configuration {

  import HelloWorld._

  implicit val timeout: Timeout = Timeout(config.getDuration("application.timeout", SECONDS).seconds)

  def boot(address: Address, handler: ActorRef) = RestApi(
    route = Some({ ec: ExecutionContext => applicationRoute(handler)(ec) })
  )

  private[api] def applicationRoute(actorRef: ActorRef)(implicit ec: ExecutionContext) = {
    path("ping" / IntNumber) { index =>
      get {
        complete {
          (actorRef ? Ping(index)).mapTo[Pong].map(_.message)
        }
      }
    }
  }

}
