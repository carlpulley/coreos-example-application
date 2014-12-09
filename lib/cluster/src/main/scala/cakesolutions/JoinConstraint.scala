package cakesolutions

import net.nikore.etcd.EtcdJsonProtocol.NodeListElement

trait JoinConstraint extends Configuration {
  this: BootableCluster =>

  def joinConstraint(joiningNodes: Seq[NodeListElement])(joinAction: => Unit): Unit

}
