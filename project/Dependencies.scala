import sbt._
import sbt.Keys._

object Dependencies {

  val scala = "2.11.4"

  object akka {
    val version = "2.3.7"

    val actor       = "com.typesafe.akka"  %% "akka-actor"   % version
    val cluster     = "com.typesafe.akka"  %% "akka-cluster" % version
    val contrib     = "com.typesafe.akka"  %% "akka-contrib" % version
    val kernel      = "com.typesafe.akka"  %% "akka-kernel"  % version
    val persistence = "com.typesafe.akka"  %% "akka-persistence-experimental" % version
    val remote      = "com.typesafe.akka"  %% "akka-remote"  % version
    val slf4j       = "com.typesafe.akka"  %% "akka-slf4j"   % version
    val testkit     = "com.typesafe.akka"  %% "akka-testkit" % version
  }

  object spray {
    val version = "1.3.2"

    val can     = "io.spray" %% "spray-can"     % version
    val httpx   = "io.spray" %% "spray-httpx"   % version
    val routing = "io.spray" %% "spray-routing" % version
    val testkit = "io.spray" %% "spray-testkit" % version
  }

  val commons       = "org.apache.commons" %  "commons-lang3"   % "3.3.2"
  val etcd          = "net.nikore.etcd"    %% "scala-etcd"      % "0.7"
  val logback       = "ch.qos.logback"     %  "logback-classic" % "1.1.2"
  val scalacheck    = "org.scalacheck"     %% "scalacheck"      % "1.11.6"
  val scalacompiler = "org.scala-lang"     % "scala-compiler"   % scala
  val scalatest     = "org.scalatest"      %% "scalatest"       % "2.2.1"
  val specs2        = "org.specs2"         %% "specs2"          % "2.4.2-scalaz-7.0.6"
  val typesafe      = "com.typesafe"       %  "config"          % "1.2.1"

}
