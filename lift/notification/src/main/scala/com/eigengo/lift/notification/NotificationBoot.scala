package com.eigengo.lift.notification

import akka.actor.ActorSystem
import akka.contrib.pattern.ClusterSharding
import cakesolutions.etcd.WithEtcd
import cakesolutions.{Configuration, MinNumJoinConstraint, BootableCluster}
import com.eigengo.lift.profile.UserProfile

class NotificationBoot extends BootableCluster(ActorSystem("Lift")) with MinNumJoinConstraint with Configuration with WithEtcd {

  cluster.registerOnMemberUp {
    // Boot the microservice when member is 'Up'
    val userProfile = ClusterSharding(system).shardRegion(UserProfile.shardName)
    val notification = system.actorOf(Notification.props(userProfile), Notification.name)
  }

}
