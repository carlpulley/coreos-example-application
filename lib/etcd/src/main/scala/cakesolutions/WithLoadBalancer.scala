package cakesolutions

import cakesolutions.etcd.WithEtcd
import scala.concurrent.ExecutionContext

trait WithLoadBalancer extends etcd.Registration {
  mixin: etcd.Registration with WithEtcd =>

  // TODO: add in code to register against a Vulcand load balancer

  protected def register()(implicit ec: ExecutionContext) = {
    mixin.register() flatMap {
      case _ =>
        etcd.setKey(???, ???)
    }
  }

  protected def unregister()(implicit ec: ExecutionContext) = {
    mixin.unregister() flatMap {
      case _ =>
        etcd.deleteKey(???)
    }
  }

}
