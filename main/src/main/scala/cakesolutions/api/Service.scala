package cakesolutions

package api

import akka.actor.{ActorRef, Address}
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import spray.routing._

trait Service extends Directives with Configuration with BootableService {

  import HelloWorld._

  implicit val timeout: Timeout = Timeout(config.getDuration("application.timeout", SECONDS).seconds)

  override def boot(address: Address, handlers: ActorRef*) = {
    require(handlers.nonEmpty, "At least one routing handler needs to be specified")

    super.boot(address, handlers: _*) + RestApi(
      route = Some({ ec: ExecutionContext => applicationRoute(handlers.head)(ec) })
    )
  }

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
