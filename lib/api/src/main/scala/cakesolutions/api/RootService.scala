package cakesolutions.api

import cakesolutions.logging.{Logging => LoggingActor}
import scala.language.implicitConversions
import scala.util.control.NonFatal
import spray.can.Http
import spray.http.HttpHeaders.RawHeader
import spray.http.StatusCodes._
import spray.http._
import spray.httpx.marshalling.{Marshaller, ToResponseMarshaller, ToResponseMarshallingContext}
import spray.routing._

class RootService(route: Route) extends LoggingActor with Directives {

  implicit def fromObjectCross[T : Marshaller](origin: Option[String])(obj: T): ToResponseMarshaller[T] =
    new ToResponseMarshaller[T] {
      def apply(value: T, ctx: ToResponseMarshallingContext): Unit = {
        new CompletionRoute(OK, RawHeader("Access-Control-Allow-Origin", origin.getOrElse("")) :: Nil, obj)
      }
    }

  private class CompletionRoute[T : Marshaller](status: StatusCode, headers: List[HttpHeader], obj: T) extends StandardRoute {
    def apply(ctx: RequestContext) {
      ctx.complete(status, headers, obj)
    }
  }

  val allCrossOrigins =
    RawHeader("Access-Control-Allow-Origin", "*") ::
      RawHeader("Access-Control-Allow-Methods", "POST") ::
      RawHeader("Access-Control-Allow-Headers", "Authorization") :: Nil

  val applicationRoute: Route =
    options {
      complete {
        HttpResponse(status = StatusCodes.OK, headers = allCrossOrigins)
      }
    } ~
      route

  implicit val handler = ExceptionHandler {
    case exn => ctx =>
      log.error(s"Unexpected exception during routing: ${exn.toString}; stack trace: ${exn.getStackTrace.toList.toString}")
      ctx.complete(InternalServerError)
  }

  def receive = {
    case _: Http.Connected =>
      sender ! Http.Register(self)

    case request: HttpRequest =>
      try {
        applicationRoute(RequestContext(request, sender(), request.uri.path).withDefaultSender(sender()))
      } catch {
        case NonFatal(exn) =>
          handler(exn)
        case exn: Throwable =>
          handler(exn)
      }
  }

}
