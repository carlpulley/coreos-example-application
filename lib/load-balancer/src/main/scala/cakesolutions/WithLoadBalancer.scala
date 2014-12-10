package cakesolutions

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

trait WithLoadBalancer extends etcd.Registration {
  mixin: etcd.Registration =>

  // TODO: add in code to register against a Vulcand load balancer

  protected def register()(implicit ec: ExecutionContext) = {
    mixin.register() andThen {
      case Success(_) =>
        ???
    }
  }

  protected def unregister()(implicit ec: ExecutionContext) = {
    mixin.unregister() andThen {
      case Success(_) =>
        ???
    }
  }

}
