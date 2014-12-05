package cakesolutions

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.MemberUp
import akka.remote.testconductor.RoleName
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.testkit.{ImplicitSender, TestProbe}
import cakesolutions.etcd.WithEtcd
import net.nikore.etcd.EtcdJsonProtocol.{NodeResponse, EtcdResponse, NodeListElement, EtcdListResponse}
import org.scalatest._
import scala.concurrent.duration._
import scala.concurrent.Future

object BootableClusterSpecConfig extends MultiNodeConfig {
  val nodes = Map(
    "node1" -> role("node1"),
    "node2" -> role("node2"),
    "node3" -> role("node3")
  )
}

class BootableClusterSpec extends MultiNodeSpec(BootableClusterSpecConfig) with WordSpecLike with Matchers with BeforeAndAfterAll with ImplicitSender {

  val initialParticipants = 0
  val retry = system.settings.config.getDuration("akka.cluster.retry", TimeUnit.SECONDS).seconds
  val etcdKey = system.settings.config.getString("akka.etcd.key")

  val etcdProbe = TestProbe()
  val mockEtcdClient = new etcd.Client("mock") {
    var lookupFailure: Boolean = true
    var etcdKVStore = Map.empty[String, String]

    override def listDir(dir: String, recursive: Boolean = false): Future[EtcdListResponse] = {
      etcdProbe.ref ! ('listDir, dir)

      if (lookupFailure) {
        Future.successful {
          EtcdListResponse("ls", NodeListElement(s"/$etcdKey", None, None, None))
        }
      } else {
        Future.successful {
          val nodes = List("node1", "node2", "node3").map(addr => NodeListElement(s"/$etcdKey/$addr", None, Some(addr), None))
          EtcdListResponse("ls", NodeListElement(s"/$etcdKey", None, None, Some(nodes)))
        }
      }
    }

    override def setKey(key: String, value: String, ttl: Option[Duration] = None) = {
      etcdProbe.ref ! ('setKey, s"$key=$value")
      etcdKVStore = etcdKVStore + (key -> value)

      Future.successful(EtcdResponse("put", NodeResponse(key, Some(value), 69, 42), None))
    }

    override def getKey(key: String) = {
      etcdProbe.ref ! ('getKey, key)

      Future.successful(EtcdResponse("get", NodeResponse(key, etcdKVStore.get(key), 69, 42), None))
    }

    override def getKeyAndWait(key: String, wait: Boolean = true) = getKey(key)

    override def deleteKey(key: String) = ???

    override def createDir(dir: String, ttl: Option[Duration] = None) = ???

    override def deleteDir(dir: String, recursive: Boolean = false) = ???
  }

  def addressKey(role: RoleName): String = {
    s"${node(role).address.host.getOrElse("")}:${node(role).address.port.getOrElse(0)}"
  }

  val joinProbe = TestProbe()
  abstract class TestBootableCluster extends BootableCluster(system) with JoinConstraint with Configuration with WithEtcd {
    def nodeName: String
    lazy val nodeRole: RoleName = BootableClusterSpecConfig.nodes(nodeName)

    override lazy val config = system.settings.config
    override lazy val etcd = mockEtcdClient

    override def joinConstraint(joiningNodes: Seq[NodeListElement])(joinAction: => Unit) = {
      joinProbe.ref ! joiningNodes

      super.joinConstraint(joiningNodes)(joinAction)
    }

    override def clusterAddressKey(): String = {
      s"${node(nodeRole).address.host.getOrElse("")}:${node(nodeRole).address.port.getOrElse(0)}"
    }
  }

  val clusterProbe = TestProbe()
  Cluster(system).subscribe(clusterProbe.ref, classOf[MemberUp])

  val node1 = new TestBootableCluster {
    val nodeName = "node1"
  }

  val node2 = new TestBootableCluster {
    val nodeName = "node2"
  }

  val node3 = new TestBootableCluster {
    val nodeName = "node3"
  }

  "BootableCluster (1 node)" should {
    "register itself with etcd on startup" in {
      node1.startup()
      within(2 * retry) {
        etcdProbe.expectMsgType[(Symbol, String)] shouldEqual('setKey, s"/$etcdKey/${addressKey(node1.nodeRole)}=Joining")
        joinProbe.expectNoMsg()
      }
    }

    "wait whilst other no other nodes join" in {
      within(2 * retry) {
        etcdProbe.expectNoMsg()
        joinProbe.expectNoMsg()
      }
    }

    "wait until at least one node is marked as 'Up'" in {
      mockEtcdClient.etcdKVStore = mockEtcdClient.etcdKVStore + (s"$etcdKey/${addressKey(node1.nodeRole)}" -> "Up")
      within(2 * retry) {
        // FIXME: need to map NodeListElement here
        joinProbe.expectMsgType[Seq[NodeListElement]].flatMap(_.value) shouldEqual Seq(node(node1.nodeRole).address)
        val msgs = clusterProbe.receiveN(1)
        msgs.map(_.asInstanceOf[MemberUp].member.address).toSet shouldEqual Set(node(node1.nodeRole).address)
      }
    }

    "de-register itself should node leave cluster" in {
      Cluster(system).leave(node(node1.nodeRole).address)
      within(2 * retry) {
        val msgs = etcdProbe.receiveN(2)
        msgs.toSet shouldEqual Set(('setKey, s"/$etcdKey/${addressKey(node1.nodeRole)}=Unreachable"), ('deleteKey, s"/$etcdKey/${addressKey(node1.nodeRole)}"))
      }
    }
  }

  "BootableCluster (2 nodes)" should {
    "register itself with etcd on startup" in {
      node2.startup()
      within(2 * retry) {
        etcdProbe.expectMsgType[(Symbol, String)] shouldEqual('setKey, s"/$etcdKey/${addressKey(node2.nodeRole)}=Joining")
        joinProbe.expectNoMsg()
      }
    }

    "wait whilst other other nodes join" in {
      node3.startup()
      within(2 * retry) {
        etcdProbe.expectMsgType[(Symbol, String)] shouldEqual('setKey, s"/$etcdKey/${addressKey(node3.nodeRole)}=Joining")
        joinProbe.expectNoMsg()
      }
    }

    "wait until at least one node is 'Up'" in {
      mockEtcdClient.etcdKVStore = mockEtcdClient.etcdKVStore + (s"$etcdKey/${addressKey(node3.nodeRole)}" -> "Up")
      within(2 * retry) {
        // FIXME: need to map NodeListElement here
        joinProbe.expectMsgType[Seq[NodeListElement]] shouldEqual Seq(node(node3.nodeRole).address)
        joinProbe.expectMsgType[Seq[NodeListElement]] shouldEqual Seq(node(node3.nodeRole).address)
        val msgs = clusterProbe.receiveN(2)
        msgs.map(_.asInstanceOf[MemberUp].member.address).toSet shouldEqual Set(node(node2.nodeRole).address, node(node3.nodeRole).address)
      }
    }

    "node3 should de-register itself from etcd on shutdown" in {
      node3.shutdown()
      within(2 * retry) {
        etcdProbe.expectMsgType[(Symbol, String)] shouldEqual('deleteKey, s"/$etcdKey/${addressKey(node3.nodeRole)}")
        joinProbe.expectNoMsg()
        etcdProbe.expectNoMsg()
      }
    }

    "node2 should remain 'Up'" in {
      within(2 * retry) {
        etcdProbe.expectNoMsg()
        joinProbe.expectNoMsg()
      }
      mockEtcdClient.etcdKVStore.get(s"/$etcdKey/${addressKey(node2.nodeRole)}") shouldEqual Some("Up")
    }
  }

}
