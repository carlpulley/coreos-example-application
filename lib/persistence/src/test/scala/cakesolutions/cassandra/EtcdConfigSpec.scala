package cakesolutions

package cassandra

import akka.actor.{Actor, Props, ActorSystem}
import akka.testkit.{TestProbe, TestKit, ImplicitSender}
import cakesolutions.etcd.WithEtcd
import com.typesafe.config.Config
import java.util.concurrent.TimeUnit
import net.nikore.etcd.EtcdJsonProtocol.{NodeListElement, EtcdListResponse}
import org.scalatest._
import scala.collection.JavaConversions._
import scala.concurrent.duration._
import scala.concurrent.Future

class EtcdConfigSpec(_system: ActorSystem) extends TestKit(_system) with WordSpecLike with Matchers with BeforeAndAfterAll with ImplicitSender {

  def this() = this(ActorSystem("TestSystem"))

  val retry = system.settings.config.getDuration("cassandra.etcd.retry", TimeUnit.SECONDS).seconds
  val etcdKey = system.settings.config.getString("cassandra.etcd.key")
  val etcdProbe = TestProbe()
  val mockEtcdClient = new etcd.Client("mock") {
    var lookupFailure: Boolean = true

    override def listDir(dir: String, recursive: Boolean = false): Future[EtcdListResponse] = {
      etcdProbe.ref ! dir
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

    override def getKey(key: String) = ???

    override def getKeyAndWait(key: String, wait: Boolean = true) = ???

    override def setKey(key: String, value: String, ttl: Option[Duration] = None) = ???

    override def deleteKey(key: String) = ???

    override def createDir(dir: String, ttl: Option[Duration] = None) = ???

    override def deleteDir(dir: String, recursive: Boolean = false) = ???
  }

  val configProbe = TestProbe()
  val storeProbe = TestProbe()
  class MockStore extends Actor {
    def receive = {
      case msg =>
        storeProbe.ref ! msg
    }
  }
  val mockProps = (config: Config) => {
    println(s"props(config) = $config")
    configProbe.ref ! config

    Props(new MockStore)
  }
  val testEtcdConfig = system.actorOf(Props(new EtcdConfig(mockProps, "dummy.config") with Configuration with WithEtcd {
    override lazy val config = system.settings.config
    override lazy val etcd = mockEtcdClient
  }), "etcd-config")

  "EtcdConfig" should {
    val period = 2 * retry // 2 attempts at 3 second retry intervals

    s"only probe etcd for /$etcdKey key" in {
      mockEtcdClient.lookupFailure = true // Ensure etcd key lookups fail
      etcdProbe.expectMsgType[String] shouldEqual s"/$etcdKey"
      configProbe.expectNoMsg(period)
      storeProbe.expectNoMsg(period)
    }

    "fail to route traffic to persistent store" in {
      testEtcdConfig ! "persistence message"
      storeProbe.expectNoMsg(period)
    }

    s"discover value for /$etcdKey key from etcd" in {
      mockEtcdClient.lookupFailure = false // Simulate an etcd key update
      within(period) {
        etcdProbe.expectMsgType[String] shouldEqual s"/$etcdKey"
        val result = configProbe.expectMsgType[Config]
        result.getStringList("contact-points").toSet shouldEqual Set("node1", "node2", "node3")
      }
    }

    "route traffic to persistent store" in {
      testEtcdConfig ! "persistence message"
      storeProbe.expectMsgType[String] shouldEqual "persistence message"
    }
  }

}
