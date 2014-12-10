package cakesolutions

import akka.actor.{ActorRef, ActorSystem}
import akka.contrib.pattern.ClusterSharding
import cakesolutions.api.WithApi
import cakesolutions.etcd.WithEtcd
import scala.concurrent.ExecutionContext

class Main extends BootableCluster(ActorSystem("HelloWorld")) with MinNumJoinConstraint with Configuration with WithEtcd with WithApi {

  def boot(handler: ActorRef) = new BootedNode with api.Service {
    override def api = Some((ec: ExecutionContext) => {
      applicationRoute(handler)(ec)
    })
  }

  cluster.registerOnMemberUp {
    // Register and boot the microservice when member is 'Up'
    val actorRef = ClusterSharding(system).start(
      typeName = HelloWorld.shardName,
      entryProps = HelloWorld.shardProps,
      idExtractor = HelloWorld.idExtractor,
      shardResolver = HelloWorld.shardResolver
    )
    boot(actorRef).api.foreach(startupApi)
  }

}
