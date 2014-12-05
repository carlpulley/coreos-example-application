package cakesolutions

package etcd

trait WithEtcd {
  this: Configuration =>

  val etcd = new Client(config.getString("etcd.url"))

}
