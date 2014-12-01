package cakesolutions

package etcd

import akka.actor.Actor
import akka.cluster.ClusterEvent._

class ClusterMonitor(key: String) extends Actor with Configuration {

  def receive = {
    case UnreachableMember(member) if key.endsWith(s"/${member.address.host.getOrElse("")}:${member.address.port.getOrElse(0)}") =>
      etcd.setKey(key, "Unreachable") // Logical state - hence use of string state value

    case MemberRemoved(member, _) if key.endsWith(s"/${member.address.host.getOrElse("")}:${member.address.port.getOrElse(0)}") =>
      etcd.deleteKey(key)
      context.stop(self)

    case MemberExited(member) if key.endsWith(s"/${member.address.host.getOrElse("")}:${member.address.port.getOrElse(0)}") =>
      etcd.deleteKey(key)
      context.stop(self)

    case MemberUp(member) if key.endsWith(s"/${member.address.host.getOrElse("")}:${member.address.port.getOrElse(0)}") =>
      etcd.setKey(key, member.status.toString)
  }

}
