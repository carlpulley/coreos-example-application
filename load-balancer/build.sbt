import Dependencies._
import sbtdocker.ImageName
import sbtdocker.Plugin.DockerKeys._
import sbtdocker.mutable.Dockerfile

Project.clusterSettings

name := "load-balancer"

mainClass in Compile := Some("cakesolutions.LoadBalancer")

libraryDependencies ++= Seq(
  // Core Akka
  akka.actor,
  akka.cluster,
  akka.kernel,
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
  scalatest     % "test",
  scalacheck    % "test",
  specs2        % "test",
  akka.testkit  % "test"
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
    repository = "load-balancer",
    tag = Some("v" + version.value))
}
