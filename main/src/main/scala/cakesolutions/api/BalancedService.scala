package cakesolutions

package api

import akka.actor.{Address, ActorRef}
import cakesolutions.etcd.WithEtcd
import cakesolutions.logging.Logger

trait BalancedService extends Service with WithLoadBalancer {
  this: WithEtcd =>

  import WithLoadBalancer._

  private val log = Logger(this.getClass())
  val balancer = LoadBalance(config.getString("application.domain"))(etcd)
  val pingUpstream = "hello-world-ping"

  override def boot(handler: ActorRef, address: Address) = super.boot(handler, address) + RestApi(
    start = Some({ () => start(address) }),
    stop  = Some({ () => stop(address) })
  )

  private[api] def start(address: Address): Unit = {
    log.debug(s"Starting REST routes using address $address")

    balancer + (MicroService("hello-world") -> Location("/ping/.*", pingUpstream))
    balancer ++ (pingUpstream -> Endpoint(s"http://${address.host.getOrElse("")}:${config.getInt("application.port")}"))
  }

  private[api] def stop(address: Address): Unit = {
    log.debug(s"Stopping REST routes using address $address")

    balancer -- (pingUpstream -> Endpoint(s"http://${address.host.getOrElse("")}:${config.getInt("application.port")}"))
  }

}
