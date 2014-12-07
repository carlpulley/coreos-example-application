package cakesolutions

import akka.actor.{ActorSystem, Props}
import cakesolutions.api.WithApi
import cakesolutions.etcd.WithEtcd
import scala.concurrent.ExecutionContext
import spray.util.LoggingContext._

class Main extends BootableCluster(ActorSystem("HelloWorld")) with JoinConstraint with Configuration with WithEtcd with WithApi {

  def boot(system: ActorSystem): BootedNode = {
    new BootedNode with api.Service {
      val applicationActor = system.actorOf(Props[HelloWorld])

      override def api = Some((ec: ExecutionContext) => applicationRoute(applicationActor)(fromAdapter(system.log), ec))
    }
  }

  cluster.registerOnMemberUp {
    // Boot the microservice when member is 'Up'
    val bootedNode = boot(system)
    bootedNode.api.foreach(startupApi)
  }

}
