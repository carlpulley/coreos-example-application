package cakesolutions

import com.typesafe.config.ConfigFactory
import net.nikore.etcd.EtcdClient

object EtcdKeys {

  val ClusterNodes = "akka.cluster.nodes"

}

trait Configuration {

  val config = ConfigFactory.load()
  val etcd = new EtcdClient(config.getString("etcd.url"))

}
