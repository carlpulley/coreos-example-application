package cakesolutions

package api

import akka.actor.{Address, ActorRef}
import cakesolutions.etcd.WithEtcd

trait BalancedService extends Service with WithLoadBalancer {
  this: WithEtcd =>

  import WithLoadBalancer._

  val balancer = LoadBalance(config.getString("application.domain"))(etcd)
  val pingUpstream = "hello-world-ping"

  override def boot(handler: ActorRef, address: Address) = super.boot(handler, address) + RestApi(
    start = Some({ () => start(address) }),
    stop  = Some({ () => stop(address) })
  )

  private[api] def start(address: Address): Unit = {
    balancer + (MicroService("hello-world") -> Location("/ping", pingUpstream))
    balancer ++ (pingUpstream -> Endpoint(s"http://${address.host.getOrElse("")}:${address.port.getOrElse(0)}"))
  }

  private[api] def stop(address: Address): Unit = {
    balancer -- (pingUpstream -> Endpoint(s"http://${address.host.getOrElse("")}:${address.port.getOrElse(0)}"))
  }

}
