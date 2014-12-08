package cakesolutions

import akka.actor.{ActorRef, ActorSystem}
import akka.contrib.pattern.ClusterSharding
import cakesolutions.api.WithApi
import cakesolutions.etcd.WithEtcd
import scala.concurrent.ExecutionContext

class Main extends BootableCluster(ActorSystem("HelloWorld")) with JoinConstraint with Configuration with WithEtcd with WithApi {

  def boot(ref: ActorRef) = new BootedNode with api.Service {
    override def api = Some((ec: ExecutionContext) => {
      applicationRoute(ref)(ec)
    })
  }

  cluster.registerOnMemberUp {
    // Register and boot the microservice when member is 'Up'
    val applicationRef = ClusterSharding(system).start(
      typeName = HelloWorld.shardName,
      entryProps = Some(HelloWorld.props),
      idExtractor = HelloWorld.idExtractor,
      shardResolver = HelloWorld.shardResolver
    )
    boot(applicationRef).api.foreach(startupApi)
  }

}
