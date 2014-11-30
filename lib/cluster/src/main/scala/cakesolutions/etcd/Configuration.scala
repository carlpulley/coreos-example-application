package cakesolutions.etcd

import cakesolutions.logging
import net.nikore.etcd.EtcdClient

trait Configuration extends logging.Configuration {

  val etcd = new Client(config.getString("etcd.url"))

}
