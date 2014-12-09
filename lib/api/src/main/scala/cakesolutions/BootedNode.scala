package cakesolutions

import scala.concurrent.ExecutionContext
import spray.http.StatusCodes._
import spray.routing.{Directives, Route, RouteConcatenation}

trait BootedNode {
  import BootedNode._

  def api: Option[RestApi] = None

  def +(that: BootedNode): BootedNode = (this.api, that.api) match {
    case (Some(r1), Some(r2)) =>
      Default(r1, r2)

    case (Some(r1), None) =>
      this

    case (None, Some(r2)) =>
      that

    case _ =>
      this
  }

  def terminate: BootedNode = this.api match {
    case Some(api) =>
      Terminate(api)

    case None =>
      this
  }
}

object BootedNode {
  type RestApi = ExecutionContext => Route

  case class Default(api1: RestApi, api2: RestApi) extends BootedNode with RouteConcatenation {
    override lazy val api = Some({ ec: ExecutionContext => api1(ec) ~ api2(ec) })
  }

  case class Terminate(api1: RestApi) extends BootedNode with Directives {
    override lazy val api = Some({ ec: ExecutionContext =>
      logRequestResponse("REST API") {
        api1(ec) ~
          complete(NotFound, "Requested resource was not found")
      }
    })

    override def +(that: BootedNode): BootedNode = this
  }
}
