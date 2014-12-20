package cakesolutions

import cakesolutions.WithLoadBalancer.{Location, MicroService, LoadBalance}
import scala.concurrent.duration._

object WithLimit {

  sealed trait LimitConfig {
    def toString: String
  }

  case class Rate(requests: Int, burst: Int, period: Duration = 1.second) extends LimitConfig {
    require(requests > 0)
    require(burst > 0)

    override def toString: String = {
      s"""
         |{
         |  "Priority": 0,
         |  "Type": "ratelimit",
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

  case class Connection(connections: Int) extends LimitConfig {
    require(connections > 0)

    override def toString: String = {
      s"""
         |{
         |  "Priority": 0,
         |  "Type": "connlimit",
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
      val limitPath = s"${limit.getClass.getName.toLowerCase}limit"
      log.debug(s"Enabling ${limit.getClass.getName.toLowerCase} limiting for $microservice -> $location location in domain $domain")

      etcd.setKey(s"/vulcand/hosts/$domain/locations/${microservice.name}/middlewares/$limitPath/cb1", limit.toString)
      this
    }

  }

}
