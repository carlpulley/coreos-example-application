package cakesolutions

import akka.actor.ActorSystem
import akka.contrib.pattern.ClusterSharding
import cakesolutions.api.WithApi
import cakesolutions.etcd.WithEtcd

class Main extends BootableCluster(ActorSystem("HelloWorld")) with api.BalancedService with MinNumJoinConstraint with Configuration with WithEtcd with WithApi {

  cluster.registerOnMemberUp {
    // Register and boot the microservice when member is 'Up'
    val handler = ClusterSharding(system).start(
      typeName = HelloWorld.shardName,
      entryProps = HelloWorld.shardProps,
      idExtractor = HelloWorld.idExtractor,
      shardResolver = HelloWorld.shardResolver
    )
    val api = boot(handler, cluster.selfAddress)

    startupApi(api)
    system.registerOnTermination {
      shutdownApi(api)
    }
  }

}
