package com.eigengo.lift.exercise.api

import akka.actor.{ActorRef, Address}
import cakesolutions.api.RestApi
import cakesolutions.etcd.WithEtcd
import cakesolutions.logging.Logger
import cakesolutions.{Configuration, WithLoadBalancer}

trait BalancedExerciseService  extends ExerciseService with WithLoadBalancer with Configuration {
  this: WithEtcd =>

  import cakesolutions.WithLoadBalancer._

  private val log = Logger(this.getClass())
  val balancer = LoadBalance(config.getString("application.domain"))(etcd)
  val exerciseUpstream = "lift-exercise"

  override def boot(address: Address, userExercises: ActorRef, userExercisesView: ActorRef, exerciseClassifiers: ActorRef) = super.boot(address, userExercises, userExercisesView, exerciseClassifiers) + RestApi(
    start = Some({ () => start(address) }),
    stop  = Some({ () => stop(address) })
  )

  private[api] def start(address: Address): Unit = {
    log.debug(s"Starting REST routes using address $address")

    balancer + (MicroService("lift-exercise") -> Location("/exercise/.*", exerciseUpstream))
    balancer ++ (exerciseUpstream -> Endpoint(s"http://${address.host.getOrElse("")}:${config.getInt("application.port")}"))
  }

  private[api] def stop(address: Address): Unit = {
    log.debug(s"Stopping REST routes using address $address")

    balancer -- (exerciseUpstream -> Endpoint(s"http://${address.host.getOrElse("")}:${config.getInt("application.port")}"))
  }

}
