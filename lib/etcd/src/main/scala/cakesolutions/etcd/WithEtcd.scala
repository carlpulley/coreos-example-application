package cakesolutions

package etcd

trait WithEtcd {
  this: Configuration =>

  lazy val etcd = new Client(config.getString("etcd.url"))

}
