package com.eigengo.lift.profile.api

import akka.actor.{ActorRef, Address}
import cakesolutions.api.RestApi
import cakesolutions.etcd.WithEtcd
import cakesolutions.logging.Logger
import cakesolutions.{Configuration, WithLoadBalancer}

trait BalancedProfileService  extends ProfileService with WithLoadBalancer with Configuration {
  this: WithEtcd =>

  import cakesolutions.WithLoadBalancer._

  private val log = Logger(this.getClass())
  val balancer = LoadBalance(config.getString("application.domain"))(etcd)
  val profileUpstream = "lift-user"

  override def boot(address: Address, userProfile: ActorRef, userProfileProcessor: ActorRef) = super.boot(address, userProfile, userProfileProcessor) + RestApi(
    start = Some({ () => start(address) }),
    stop  = Some({ () => stop(address) })
  )

  private[api] def start(address: Address): Unit = {
    log.debug(s"Starting REST routes using address $address")

    balancer + (MicroService("lift-profile") -> Location("/user/.*", profileUpstream))
    balancer ++ (profileUpstream -> Endpoint(s"http://${address.host.getOrElse("")}:${config.getInt("application.port")}"))
  }

  private[api] def stop(address: Address): Unit = {
    log.debug(s"Stopping REST routes using address $address")

    balancer -- (profileUpstream -> Endpoint(s"http://${address.host.getOrElse("")}:${config.getInt("application.port")}"))
  }

}
