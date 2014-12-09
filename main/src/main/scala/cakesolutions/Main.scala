package cakesolutions

import akka.actor.ActorSystem
import akka.contrib.pattern.ClusterSharding
import cakesolutions.api.WithApi
import cakesolutions.etcd.WithEtcd
import scala.concurrent.ExecutionContext

class Main extends BootableCluster(ActorSystem("HelloWorld")) with MinNumJoinConstraint with Configuration with WithEtcd with WithApi {

  def boot = new BootedNode with api.Service {
    override def api = Some((ec: ExecutionContext) => {
      // As shard regions may be subjected to re-balancing, we dynamically lookup the location of a shard region
      applicationRoute(ClusterSharding(system).shardRegion(HelloWorld.shardName))(ec)
    })
  }

  cluster.registerOnMemberUp {
    // Register and boot the microservice when member is 'Up'
    ClusterSharding(system).start(
      typeName = HelloWorld.shardName,
      entryProps = HelloWorld.shardProps,
      idExtractor = HelloWorld.idExtractor,
      shardResolver = HelloWorld.shardResolver
    )
    boot.api.foreach(startupApi)
  }

}
