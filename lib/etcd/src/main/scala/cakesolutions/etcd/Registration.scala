package cakesolutions.etcd

import net.nikore.etcd.EtcdJsonProtocol.EtcdResponse
import scala.concurrent.{ExecutionContext, Future}

trait Registration {
  protected def register()(implicit ec: ExecutionContext): Future[EtcdResponse]

  protected def unregister()(implicit ec: ExecutionContext): Future[EtcdResponse]
}
