package cakesolutions.api

import spray.http.StatusCodes._
import spray.routing.{Directives, Route}

import scala.concurrent.ExecutionContext

case class RestApi(route: Option[ExecutionContext => Route] = None, start: Option[() => Unit] = None, stop: Option[() => Unit] = None) extends Directives {
  private def routeConcat(r1: ExecutionContext => Route, r2: ExecutionContext => Route): ExecutionContext => Route =
    { ec: ExecutionContext => r1(ec) ~ r2(ec) }

  private def actionConcat(a1: () => Unit, a2: () => Unit): () => Unit =
    { () => a1(); a2() }

  def +(that: RestApi): RestApi = RestApi(
    route = (this.route ++ that.route).reduceOption(routeConcat),
    start = (this.start ++ that.start).reduceOption(actionConcat),
    stop  = (this.stop ++ that.stop).reduceOption(actionConcat)
  )

  def terminate: RestApi = this.copy(
    route = this.route.map {
      case route => { ec: ExecutionContext =>
        logRequestResponse("REST API") {
          route(ec) ~
            complete(NotFound, "Requested resource was not found")
        }
      }
    }
  )
}
