package com.eigengo.lift.notification

import akka.actor.ActorSystem
import cakesolutions.etcd.WithEtcd
import cakesolutions.{Configuration, MinNumJoinConstraint, BootableCluster}

class NotificationBoot extends BootableCluster(ActorSystem("Lift")) with MinNumJoinConstraint with Configuration with WithEtcd {

  cluster.registerOnMemberUp {
    // Boot the microservice when member is 'Up'
    val userProfile = system.actorSelection("profile") // FIXME:
    val notification = system.actorOf(Notification.props(userProfile), Notification.name)
  }

}
