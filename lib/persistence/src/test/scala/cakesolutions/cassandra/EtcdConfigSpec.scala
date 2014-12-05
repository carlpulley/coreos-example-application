package cakesolutions

package cassandra

import akka.actor.{Actor, Props, ActorSystem}
import akka.testkit.{TestActorRef, TestProbe, TestKit, ImplicitSender}
import cakesolutions.etcd.WithEtcd
import com.typesafe.config.{ConfigFactory, Config}
import net.nikore.etcd.EtcdJsonProtocol.{Error, NodeListElement, EtcdListResponse}
import org.scalatest._
import scala.concurrent.duration._
import scala.concurrent.Future

class EtcdConfigSpec extends TestKit(ActorSystem("TestSystem", ConfigFactory.parseString(
  """
    |cassandra.etcd.key = "cassandra"
    |cassandra.etcd.retry = 3 s
    |dummy.config = {
    | name = "Dummy Config"
    |}
  """.stripMargin))) with WordSpecLike with Matchers with BeforeAndAfterAll with ImplicitSender {

  import system.dispatcher

  val etcdProbe = TestProbe()
  val mockEtcdClient = new etcd.Client("mock") {
    var count = 0

    override def listDir(dir: String, recursive: Boolean = false): Future[EtcdListResponse] = {
      etcdProbe.ref ! dir
      if (count < 3) {
        count += 1
        Future {
          Error(42, "Key not found", "Key not found", 2)
        }
      } else {
        Future {
          val nodes = List().map(addr => NodeListElement(s"/cassandra/$addr", None, Some(addr), None))
          EtcdListResponse("ls", NodeListElement("/cassandra", None, None, Some(nodes)))
        }
      }
    }
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
    configProbe.ref ! config

    Props(new MockStore)
  }
  val testEtcdConfig = TestActorRef(new EtcdConfig(mockProps, "dummy.config") with Configuration with WithEtcd {
    override val etcd = mockEtcdClient
  })

  "" should {
    "" in {
      val period = 3*5
      etcdProbe.expectMsgType[String] must be("/cassandra")
      configProbe.expectNoMsg(period.seconds)
      storeProbe.expectNoMsg(period.seconds)
      within(period.seconds) {
        etcdProbe.expectMsgType[String] must be("/cassandra")
        configProbe.expectMsgType[Config]
      }
      testEtcdConfig ! ???
    }
  }

}
