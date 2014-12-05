package cakesolutions

package cassandra

package snapshot

import akka.actor.Props
import akka.persistence.cassandra.snapshot.{CassandraSnapshotStoreConfig, ConfiguredCassandraSnapshotStore}
import cakesolutions.etcd.WithEtcd

class EtcdSnapshotStore extends EtcdConfig(c => Props(new ConfiguredCassandraSnapshotStore { val config = new CassandraSnapshotStoreConfig(c); init() }), "cassandra-snapshot-store") with Configuration with WithEtcd
