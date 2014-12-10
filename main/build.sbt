import Dependencies._
import sbtdocker.ImageName
import sbtdocker.Plugin.DockerKeys._
import sbtdocker.mutable.Dockerfile

Project.settings

name := "main"

mainClass in Compile := Some("cakesolutions.Main")

libraryDependencies ++= Seq(
  // Core Akka
  akka.actor,
  akka.cluster,
  akka.contrib,
  akka.kernel,
  akka.persistence,
  akka.persistence_cassandra,
  akka.remote,
  akka.slf4j,
  // REST API
  spray.httpx,
  spray.can,
  spray.routing,
  // Miscellaneous
  commons,
  etcd,
  logback,
  typesafe,
  // Testing
  scalatest    % "test",
  scalacheck   % "test",
  specs2       % "test",
  akka.testkit % "test"
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
    repository = "helloworld",
    tag = Some("v" + version.value))
}
