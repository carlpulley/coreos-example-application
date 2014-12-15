package com.eigengo.lift.profile

import akka.actor.ActorSystem
import akka.contrib.pattern.ClusterSharding
import cakesolutions.api.WithApi
import cakesolutions.etcd.WithEtcd
import cakesolutions.{Configuration, MinNumJoinConstraint, BootableCluster}

class ProfileBoot extends BootableCluster(ActorSystem("Lift")) with api.BalancedProfileService with MinNumJoinConstraint with Configuration with WithEtcd with WithApi {

  cluster.registerOnMemberUp {
    // Register and boot the microservices when member is 'Up'
    val userProfile = ClusterSharding(system).start(
      typeName = UserProfile.shardName,
      entryProps = UserProfile.shardProps,
      idExtractor = UserProfile.idExtractor,
      shardResolver = UserProfile.shardResolver
    )
    val userProfileProcessor = system.actorOf(UserProfileProcessor.props(userProfile), UserProfileProcessor.name)
    val api = boot(cluster.selfAddress, userProfile, userProfileProcessor)

    startupApi(api)
    system.registerOnTermination {
      shutdownApi(api)
    }
  }

}
