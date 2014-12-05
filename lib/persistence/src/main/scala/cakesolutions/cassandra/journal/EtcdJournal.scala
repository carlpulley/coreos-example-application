package cakesolutions

package cassandra

package journal

import akka.actor.Props
import akka.persistence.cassandra.journal.{CassandraJournalConfig, ConfiguredCassandraJournal}
import cakesolutions.etcd.WithEtcd

class EtcdJournal extends EtcdConfig(c => Props(new ConfiguredCassandraJournal { val config = new CassandraJournalConfig(c); init() }), "cassandra-journal") with Configuration with WithEtcd
