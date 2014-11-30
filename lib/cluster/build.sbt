import Dependencies._

Project.settings

name := "cluster"

libraryDependencies ++= Seq(
  // Core Akka
  akka.actor,
  akka.cluster,
  akka.contrib,
  akka.kernel,
  akka.remote,
  akka.slf4j,
  // Miscellaneous
  commons,
  etcd,
  logback,
  typesafe,
  // Testing
  scalatest     % "test",
  scalacheck    % "test",
  specs2        % "test",
  akka.testkit  % "test"
)
