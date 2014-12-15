import Dependencies._

Project.clusterSettings

name := "persistence"

libraryDependencies ++= Seq(
  // Core Akka
  akka.actor,
  akka.contrib,
  akka.persistence,
  akka.persistence_cassandra,
  akka.remote,
  akka.slf4j,
  // Miscellaneous
  commons,
  logback,
  typesafe
)
