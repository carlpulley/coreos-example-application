package cakesolutions

import scala.concurrent.ExecutionContext
import spray.routing.{Route, RouteConcatenation}

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
}

object BootedNode {
  type RestApi = ExecutionContext => Route

  case class Default(api1: RestApi, api2: RestApi) extends BootedNode with RouteConcatenation {
    override lazy val api = Some({ ec: ExecutionContext => api1(ec) ~ api2(ec) })
  }
}
