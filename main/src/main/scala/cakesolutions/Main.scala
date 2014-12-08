package cakesolutions

import akka.actor.{ActorSystem, Props}
import akka.contrib.pattern.ClusterSharding
import cakesolutions.api.WithApi
import cakesolutions.etcd.WithEtcd
import scala.concurrent.ExecutionContext

class Main extends BootableCluster(ActorSystem("HelloWorld")) with JoinConstraint with Configuration with WithEtcd with WithApi {

  val bootedNode = new BootedNode with api.Service {
    override def api = Some((ec: ExecutionContext) => {
      //val applicationActor = system.actorOf(Props[HelloWorld])
      val applicationActor = ClusterSharding(system).start(
        typeName = "hello-world",
        entryProps = Some(Props[HelloWorld]),
        idExtractor = HelloWorld.idExtractor,
        shardResolver = HelloWorld.shardResolver
      )

      applicationRoute(applicationActor)(ec)
    })
  }

  cluster.registerOnMemberUp {
    // Boot the microservice when member is 'Up'
    bootedNode.api.foreach(startupApi)
  }

}
