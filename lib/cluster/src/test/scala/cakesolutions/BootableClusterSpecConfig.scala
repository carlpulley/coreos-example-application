package cakesolutions

import akka.actor.{ActorSystem, Address}
import akka.testkit.TestProbe
import net.nikore.etcd.EtcdJsonProtocol.{NodeResponse, EtcdResponse, NodeListElement, EtcdListResponse}
import scala.concurrent.Future
import scala.concurrent.duration.Duration

object BootableClusterSpecConfig {
  implicit val system = ActorSystem("TestSystem")

  val etcdKey: String = system.settings.config.getString("akka.etcd.key")

  val etcdProbe = TestProbe()
  val mockEtcdClient = new etcd.Client("mock") {
    var lookupFailure: Boolean = true
    var etcdKVStore = Map.empty[String, String]

    override def listDir(dir: String, recursive: Boolean = false): Future[EtcdListResponse] = {
      etcdProbe.ref ! ('listDir, dir)

      if (lookupFailure) {
        Future.successful {
          EtcdListResponse("ls", NodeListElement(s"$etcdKey/", None, None, None))
        }
      } else {
        Future.successful {
          val nodes = etcdKVStore.keys.filter(_.startsWith(s"$etcdKey/")).map(key => NodeListElement(key.stripPrefix(s"$etcdKey/"), None, etcdKVStore.get(key), None)).toList
          EtcdListResponse("ls", NodeListElement(s"$etcdKey/", None, None, Some(nodes)))
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

    override def deleteKey(key: String) = {
      etcdProbe.ref ! ('deleteKey, key)
      etcdKVStore = etcdKVStore - key

      Future.successful(EtcdResponse("delete", NodeResponse(key, None, 69, 42), None))
    }

    override def createDir(dir: String, ttl: Option[Duration] = None) = ???

    override def deleteDir(dir: String, recursive: Boolean = false) = ???
  }

  def addressKey(addr: Address): String = {
    s"${addr.host.getOrElse("")}:${addr.port.getOrElse(0)}"
  }

  val joinProbe = TestProbe()
}
