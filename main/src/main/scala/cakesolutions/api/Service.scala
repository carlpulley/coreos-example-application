package cakesolutions

package api

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import java.util.concurrent.TimeUnit._
import scala.concurrent.ExecutionContext
import spray.http.StatusCodes._
import spray.routing._
import spray.util.LoggingContext

trait Service extends Directives with Configuration with ExceptionLogging {

  import HelloWorld._

  implicit val timeout: Timeout = Timeout(config.getDuration("application.timeout", SECONDS), SECONDS)

  implicit def rejectionHandler(implicit log: LoggingContext): RejectionHandler = RejectionHandler {
    case AuthenticationFailedRejection(_, _) :: _ =>
      complete(Unauthorized)
  }

  implicit def exceptionHandler(implicit log: LoggingContext): ExceptionHandler = ExceptionHandler {
    case exn: Throwable =>
      log.error(s"Unexpected exception during routing: ${exceptionString(exn)}")
      complete(InternalServerError)
  }

  def applicationRoute(actorRef: ActorRef)(implicit log: LoggingContext, ec: ExecutionContext): Route = {
    logRequestResponse("CoreOS Application API") {
      pathPrefix("ping") {
        pathEndOrSingleSlash {
          get {
            complete {
              (actorRef ? Ping).mapTo[Pong].map(_.message)
            }
          }
        }
      } ~
        complete {
          (NotFound, "Requested resource was not found")
        }
    }
  }
}
