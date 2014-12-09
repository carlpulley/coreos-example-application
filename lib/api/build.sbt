import Dependencies._

Project.clusterSettings

name := "api"

libraryDependencies ++= Seq(
  // Core Akka
  akka.actor,
  akka.slf4j,
  // Miscellaneous
  commons,
  logback,
  typesafe,
  // REST API
  spray.httpx,
  spray.can,
  spray.routing,
  // Testing
  scalatest     % "test",
  scalacheck    % "test",
  specs2        % "test"
)
