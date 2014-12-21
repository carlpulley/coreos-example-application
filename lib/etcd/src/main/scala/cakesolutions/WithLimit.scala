package cakesolutions

import cakesolutions.WithLoadBalancer.{Location, MicroService, LoadBalance}
import java.util.UUID
import scala.concurrent.duration._

object WithLimit {

  sealed trait LimitConfig {
    def id: UUID
    def `type`: String
    def toString: String
  }

  case class Rate(requests: Int, burst: Int, period: Duration = 1.second, id: UUID = UUID.randomUUID()) extends LimitConfig {
    require(requests > 0)
    require(burst > 0)

    val `type` = "ratelimit"

    override def toString: String = {
      s"""
         |{
         |  "Priority": 0,
         |  "Type": "${`type`}",
         |  "Middleware":{
         |    "Requests": $requests,
         |    "PeriodSeconds": ${period.toSeconds},
         |    "Burst": $burst,
         |    "Variable": "client.ip"
         |  }
         |}
       """.stripMargin
    }
  }

  case class Connection(connections: Int, id: UUID = UUID.randomUUID()) extends LimitConfig {
    require(connections > 0)

    val `type` = "connlimit"

    override def toString: String = {
      s"""
         |{
         |  "Priority": 0,
         |  "Type": "${`type`}",
         |  "Middleware":{
         |    "Connections":$connections,
         |    "Variable": "client.ip"
         |  }
         |}
       """.stripMargin
    }
  }

  trait Limit extends Configuration {
    mixin: LoadBalance =>

    def +(mapping: (MicroService, Location), limit: LimitConfig): LoadBalance = {
      mixin.+(mapping)
      val (microservice, location) = mapping
      log.debug(s"Enabling ${limit.getClass.getName.toLowerCase} limiting for $microservice -> $location location in domain $domain")

      etcd.setKey(s"/vulcand/hosts/$domain/locations/${microservice.name}/middlewares/${limit.`type`}/${limit.id}", limit.toString)
      this
    }

  }

}
