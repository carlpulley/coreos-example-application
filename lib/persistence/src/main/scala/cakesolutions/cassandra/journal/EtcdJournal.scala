package cakesolutions.cassandra

package journal

import akka.actor.Props
import akka.persistence.cassandra.journal.{CassandraJournalConfig, ConfiguredCassandraJournal}

class EtcdJournal extends EtcdConfig(c => Props(new ConfiguredCassandraJournal { val config = new CassandraJournalConfig(c); init() }), "cassandra-journal")
