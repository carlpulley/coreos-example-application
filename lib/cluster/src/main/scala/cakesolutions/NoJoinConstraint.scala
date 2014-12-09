package cakesolutions

import net.nikore.etcd.EtcdJsonProtocol.NodeListElement

trait NoJoinConstraint extends JoinConstraint {
  this: BootableCluster =>

  def joinConstraint(joiningNodes: Seq[NodeListElement])(joinAction: => Unit) = {
    joinAction
  }

}
