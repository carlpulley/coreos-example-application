package com.eigengo.lift.notification

import akka.actor.{ActorRef, ActorSystem}
import cakesolutions.api.WithApi
import cakesolutions.etcd.WithEtcd
import cakesolutions.{Configuration, MinNumJoinConstraint, BootableCluster}
import com.eigengo.lift.common.MicroserviceApp.BootedNode
import com.eigengo.lift.profile.{UserProfileProcessor, UserProfile, api}

case class NotificationBoot(notification: ActorRef) extends BootedNode

object NotificationBoot {

  def boot(userProfile: ActorRef)(implicit system: ActorSystem): NotificationBoot = {
    val notification = system.actorOf(Notification.props(userProfile), Notification.name)
    NotificationBoot(notification)
  }

}
