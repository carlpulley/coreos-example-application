import Dependencies._

Project.clusterSettings

name := "lift-common"

libraryDependencies ++= Seq(
  // Core Akka
  akka.actor,
  akka.cluster,
  akka.contrib,
  akka.persistence,
  scalaz.core,
  // Spray + Json
  spray.routing,
  spray.can,
  // Scodec
  scodec_bits
)
