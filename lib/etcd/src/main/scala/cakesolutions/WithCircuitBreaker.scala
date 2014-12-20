package cakesolutions

import cakesolutions.WithLoadBalancer._
import cakesolutions.etcd.WithEtcd
import java.util.concurrent.TimeUnit
import scala.concurrent.duration._

trait WithCircuitBreaker {
  this: WithLoadBalancer with WithEtcd with Configuration =>

  import WithCircuitBreaker._

  override def balancer: LoadBalance with CircuitBreaker

}

object WithCircuitBreaker {

  sealed trait Fallback {
    def toString: String
  }

  case class Response(message: String, code: Int = 400) extends Fallback {
    override def toString: String = {
      s"""
         |{
         |   "Type": "response",
         |   "Action": {
         |      "ContentType": "text/plain",
         |      "StatusCode": $code,
         |      "Body": "$message"
         |   }
         |}
       """.stripMargin
    }
  }

  case class Redirect(url: String) extends Fallback {
    override def toString: String = {
      s"""
         |{
         |   "Type": "redirect",
         |   "Action": {
         |     "URL": "$url"
         |   }
         |}
       """.stripMargin
    }
  }

  trait CircuitBreaker extends Configuration {
    this: LoadBalance =>

    def +(mapping: (MicroService, Location), fallback: Fallback = Response("Please try again latter")): LoadBalance with CircuitBreaker = {
      val (microservice, location) = mapping
      val cbreaker =
        s"""
        |{
        |  "Id":"cb1",
        |  "Priority":1,
        |  "Type":"cbreaker",
        |  "Middleware":{
        |     "Condition":"${config.getString("vulcand.circuit_breaker.condition")}",
        |     "Fallback":${fallback.toString},
        |     "FallbackDuration": ${config.getDuration("vulcand.circuit_breaker.fallback.duration", TimeUnit.SECONDS).microseconds},
        |     "RecoveryDuration": ${config.getDuration("vulcand.circuit_breaker.recovery", TimeUnit.SECONDS).microseconds},
        |     "CheckPeriod": ${config.getDuration("vulcand.circuit_breaker.check", TimeUnit.SECONDS).microseconds}
        |  }
        |}
      """.stripMargin
      log.debug(s"Enabling circuit breakers for $microservice -> $location location in domain $domain")

      etcd.setKey(s"/vulcand/hosts/$domain/locations/${microservice.name}/middlewares/cbreaker/cb1", cbreaker)
      this
    }

  }

}
