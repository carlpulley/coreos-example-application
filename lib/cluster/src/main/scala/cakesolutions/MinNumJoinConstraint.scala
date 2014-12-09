package cakesolutions

import net.nikore.etcd.EtcdJsonProtocol.NodeListElement

trait MinNumJoinConstraint extends JoinConstraint {
  this: BootableCluster =>

  import system.dispatcher

  val minNumNodes = config.getInt("akka.cluster.min-nr-of-members")

  def joinConstraint(joiningNodes: Seq[NodeListElement])(joinAction: => Unit): Unit = {
    if (joiningNodes.size >= minNumNodes) {
      joinAction
    } else {
      log.warning(s"Not enough joining nodes found - retrying in $retry seconds")
      system.scheduler.scheduleOnce(retry)(joinCluster())
    }
  }

}
