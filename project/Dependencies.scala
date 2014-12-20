import sbt._
import sbt.Keys._

object Dependencies {

  val scala = "2.11.4"

  object akka {
    val version = "2.3.7"

    val actor                 = "com.typesafe.akka"   %% "akka-actor"                    % version
    val cluster               = "com.typesafe.akka"   %% "akka-cluster"                  % version
    val contrib               = "com.typesafe.akka"   %% "akka-contrib"                  % version intransitive()
    val kernel                = "com.typesafe.akka"   %% "akka-kernel"                   % version
    val multi_node_testkit    = "com.typesafe.akka"   %% "akka-multi-node-testkit"       % version
    val persistence           = "com.typesafe.akka"   %% "akka-persistence-experimental" % version intransitive()
    val persistence_cassandra = "com.github.krasserm" %% "akka-persistence-cassandra"    % "0.3.4" intransitive()
    val remote                = "com.typesafe.akka"   %% "akka-remote"                   % version
    val slf4j                 = "com.typesafe.akka"   %% "akka-slf4j"                    % version
    val testkit               = "com.typesafe.akka"   %% "akka-testkit"                  % version
  }

  object json4s {
    val native = "org.json4s" %% "json4s-native" % "3.2.11"
  }

  object spray {
    val version = "1.3.2"

    val can     = "io.spray" %% "spray-can"                % version
    val httpx   = "io.spray" %% "spray-httpx"              % version
    val routing = "io.spray" %% "spray-routing-shapeless2" % version
    val testkit = "io.spray" %% "spray-testkit"            % version
  }

  object scalaz {
    val version = "7.1.0"

    val core = "org.scalaz" %% "scalaz-core"  % version
  }

  val apns             = "com.notnoop.apns"       % "apns"                   % "0.1.6" // Apple push notifications
  val cassandra_driver = "com.datastax.cassandra" %  "cassandra-driver-core" % "2.1.3"
  val commons          = "org.apache.commons"     %  "commons-lang3"         % "3.3.2"
  val etcd             = "net.nikore.etcd"        %% "scala-etcd"            % "0.7"
  val logback          = "ch.qos.logback"         %  "logback-classic"       % "1.1.2"
  val scalacheck       = "org.scalacheck"         %% "scalacheck"            % "1.11.6"
  val scalacompiler    = "org.scala-lang"         %  "scala-compiler"        % scala
  val scalatest        = "org.scalatest"          %% "scalatest"             % "2.2.1"
  val specs2           = "org.specs2"             %% "specs2"                % "3.0-M1"
  val typesafe         = "com.typesafe"           %  "config"                % "1.2.1"

}
