package com.eigengo.lift.exercise

import akka.actor.ActorSystem
import akka.contrib.pattern.ClusterSharding
import cakesolutions.api.WithApi
import cakesolutions.etcd.WithEtcd
import cakesolutions._

class ExerciseBoot extends BootableCluster(ActorSystem("Lift")) with api.BalancedExerciseService with MinNumJoinConstraint with Configuration with WithEtcd with WithApi {

  cluster.registerOnMemberUp {
    // Register and boot the microservices when member is 'Up'
    val notification = system.actorSelection("notification") // FIXME:
    val exerciseClassifiers = system.actorOf(ExerciseClassifiers.props, ExerciseClassifiers.name)
    val userExercise = ClusterSharding(system).start(
      typeName = UserExercises.shardName,
      entryProps = UserExercises.shardProps(notification, exerciseClassifiers),
      idExtractor = UserExercises.idExtractor,
      shardResolver = UserExercises.shardResolver
    )
    val userExerciseView = ClusterSharding(system).start(
      typeName = UserExercisesView.shardName,
      entryProps = UserExercisesView.shardProps,
      idExtractor = UserExercisesView.idExtractor,
      shardResolver = UserExercisesView.shardResolver
    )
    val api = boot(cluster.selfAddress, userExercise, userExerciseView, exerciseClassifiers)

    startupApi(api)
    system.registerOnTermination {
      shutdownApi(api)
    }
  }

}

