import Dependencies._

Project.settings

name := "logging"

libraryDependencies ++= Seq(
  // Core Akka
  akka.actor,
  akka.slf4j,
  // Miscellaneous
  commons,
  logback,
  typesafe,
  // Testing
  scalatest     % "test",
  scalacheck    % "test",
  specs2        % "test",
  akka.testkit  % "test"
)
