package cakesolutions

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.MemberUp
import akka.testkit.{TestKit, ImplicitSender, TestProbe}
import cakesolutions.etcd.WithEtcd
import java.util.concurrent.TimeUnit
import net.nikore.etcd.EtcdJsonProtocol.NodeListElement
import org.scalatest._
import scala.concurrent.duration._
import scala.language.reflectiveCalls

class TestBootableCluster(system: ActorSystem) extends BootableCluster(system) with JoinConstraint with Configuration with WithEtcd {
  import BootableClusterSpecConfig._

  override lazy val etcd = mockEtcdClient

  override def joinConstraint(joiningNodes: Seq[NodeListElement])(joinAction: => Unit) = {
    joinProbe.ref ! joiningNodes

    super.joinConstraint(joiningNodes)(joinAction)
  }
}

class BootableClusterSpec extends TestKit(BootableClusterSpecConfig.system) with WordSpecLike with Matchers with BeforeAndAfterAll with ImplicitSender {

  import BootableClusterSpecConfig._

  val initialParticipants = 0

  val retry = system.settings.config.getDuration("akka.cluster.retry", TimeUnit.SECONDS).seconds
  val boot1 = new TestBootableCluster(system)

  val clusterProbe = TestProbe()(system)
  Cluster(system).subscribe(clusterProbe.ref, classOf[MemberUp])

  "BootableCluster (1 node)" should {
    "register itself with etcd on startup" in {
      boot1.startup()
      within(2 * retry) {
        etcdProbe.expectMsgType[(Symbol, String)] shouldEqual('setKey, s"$etcdKey/${addressKey(Cluster(system).selfAddress)}=Joining")
        joinProbe.expectNoMsg()
      }
    }

    "wait whilst other no other nodes join" in {
      within(2 * retry) {
        etcdProbe.receiveN(2).toSet shouldEqual Set(('listDir, etcdKey))
        joinProbe.expectNoMsg()
      }
    }

    "wait until at least one node is marked as 'Up'" in {
      mockEtcdClient.etcdKVStore = mockEtcdClient.etcdKVStore + (s"$etcdKey/${addressKey(Cluster(system).selfAddress)}" -> "Up")
      mockEtcdClient.lookupFailure = false
      within(2 * retry) {
        joinProbe.expectMsgType[Seq[NodeListElement]].map(_.key) shouldEqual Seq(addressKey(Cluster(system).selfAddress))
      }
      Thread.sleep(2000)
    }

    "on leaving the cluster, node should de-register from etcd" in {
      Cluster(system).leave(Cluster(system).selfAddress)
      within(2 * retry) {
        val msgs = etcdProbe.receiveWhile() {
          case m @ ('listDir, _) =>
            m
          case m @ ('setKey, _) =>
            m
          case m @ ('deleteKey, _) =>
            m
        }
        msgs.toSet should contain ('deleteKey, s"$etcdKey/${addressKey(Cluster(system).selfAddress)}")
      }
    }
  }

}
