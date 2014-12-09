package cakesolutions.api

import cakesolutions.logging.Logging
import spray.http.StatusCodes._
import spray.routing._

class RestService(route: Route) extends HttpServiceActor with Logging {

  implicit val rejectionHandler: RejectionHandler = RejectionHandler {
    case AuthenticationFailedRejection(_, _) :: _ =>
      complete(Unauthorized)
  }

  implicit val exceptionHandler = ExceptionHandler {
    case exn =>
      log.error(s"Unexpected exception during routing: ${exceptionString(exn)}")
      complete(InternalServerError)
  }

  def receive = runRoute(route)

}
