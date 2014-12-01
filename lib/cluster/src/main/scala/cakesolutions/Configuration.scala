package cakesolutions

import cakesolutions.etcd.Client

trait Configuration extends logging.Configuration {

  val etcd = new Client(config.getString("etcd.url"))

}
