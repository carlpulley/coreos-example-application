import sbt._
import sbt.Keys._
import Dependencies._
import DockerKeys._
import sbtdocker.ImageName
import sbtdocker.mutable.Dockerfile

name := "coreos-application"

organization := "cakesolutions"

version := "0.1.0-SNAPSHOT"

mainClass in Compile := Some("cakesolutions.Main")

scalaVersion := "2.11.4"

scalacOptions ++= Seq(
  "-deprecation"
  ,"-unchecked"
  ,"-feature"
  ,"-encoding", "UTF-8"
  ,"-Xlint"
)

javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")

libraryDependencies ++= Seq(
  // Core Akka
  akka.actor,
  akka.cluster,
  akka.contrib,
  akka.kernel,
  //akka.persistence,
  akka.remote,
  akka.slf4j,
  // For REST API
  spray.httpx,
  spray.can,
  spray.routing,
  // Miscellaneous
  commons,
  etcd,
  logback,
  typesafe,
  // Testing
  //scalatest % "test",
  //scalacheck % "test",
  akka.testkit % "test",
  spray.testkit % "test"
)

enablePlugins(AkkaAppPackaging)

sbtdocker.Plugin.dockerSettings

docker <<= docker.dependsOn(stage in Compile)

dockerfile in docker <<= (name, stagingDirectory in Universal) map {
  case (appName, stageDir) =>
    val workingDir = s"/app/$appName"
    new Dockerfile {
      // Use a base image that contain Java
      from("dockerfile/java")
      maintainer("Carl Pulley <carlp@cakesolutions.net>")
      // Expose port HTTP (80)
      expose(80)
      //expose(12552)
      //expose(12553)
      //expose(12554)
      // Add the libs dir
      add(stageDir, workingDir)
      run("chmod",  "+x",  s"$workingDir/bin/$appName")
      workDir(workingDir)
      entryPointShell(s"bin/$appName")
    }
}

imageName in docker := {
  ImageName(
    namespace = Some("carlpulley"),
    repository = name.value,
    tag = Some("v" + version.value))
}
