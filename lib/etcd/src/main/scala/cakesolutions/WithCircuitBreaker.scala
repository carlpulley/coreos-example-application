package cakesolutions

import cakesolutions.WithLoadBalancer._
import java.util.concurrent.TimeUnit
import java.util.UUID
import scala.concurrent.duration._

object WithCircuitBreaker {

  sealed trait Fallback {
    def id: UUID
    def toString: String
  }

  case class Response(id: UUID, message: String = "Please try again latter", code: Int = 400) extends Fallback {
    require(code > 0)

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

  case class Redirect(id: UUID, url: String) extends Fallback {
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
    mixin: LoadBalance =>

    def +(mapping: (MicroService, Location), fallback: Fallback): LoadBalance with CircuitBreaker = {
      mixin.+(mapping)
      val (microservice, location) = mapping
      val cbreaker =
        s"""
        |{
        |  "Id":"${fallback.id}",
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

      etcd.setKey(s"/vulcand/hosts/$domain/locations/${microservice.name}/middlewares/cbreaker/${fallback.id}", cbreaker)
      this
    }

  }

}
