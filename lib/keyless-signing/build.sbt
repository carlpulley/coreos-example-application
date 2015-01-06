import Dependencies._

Project.clusterSettings

name := "keyless-signing"

libraryDependencies ++= Seq(
  // Core Akka
  akka.actor,
  akka.contrib,
  akka.persistence,
  akka.persistence_cassandra,
  akka.remote,
  akka.slf4j,
  // Miscellaneous
  cassandra_driver,
  commons,
  joda_time,
  logback,
  scalaz.core,
  typesafe,
  // Testing
  scalatest     % "test",
  scalacheck    % "test",
  specs2        % "test",
  akka.testkit  % "test"
)
