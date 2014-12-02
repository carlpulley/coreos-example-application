package cakesolutions

package etcd

import akka.cluster.ClusterEvent._
import akka.cluster.Member
import akka.event.LoggingReceive
import cakesolutions.logging.{Logging => LoggingActor}

class ClusterMonitor(etcd: Client, key: String) extends LoggingActor {

  def clusterAddressKey(member: Member): String = {
    s"/${member.address.host.getOrElse("")}:${member.address.port.getOrElse(0)}"
  }

  def receive = LoggingReceive {
    case UnreachableMember(member) if key.endsWith(clusterAddressKey(member)) =>
      etcd.setKey(key, "Unreachable") // Logical state - hence use of string state value

    case MemberRemoved(member, _)  if key.endsWith(clusterAddressKey(member)) =>
      etcd.deleteKey(key)

    case MemberExited(member)      if key.endsWith(clusterAddressKey(member)) =>
      etcd.deleteKey(key)

    case MemberUp(member)          if key.endsWith(clusterAddressKey(member)) =>
      etcd.setKey(key, member.status.toString)

    case _ =>
      // Intentionally ignoring all other messages!
  }

}
