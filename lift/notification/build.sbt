import Dependencies._
import sbtdocker.ImageName
import sbtdocker.Plugin.DockerKeys._
import sbtdocker.mutable.Dockerfile

Project.clusterSettings

name := "lift-notification"

libraryDependencies ++= Seq(
  akka.actor,
  akka.cluster,
  akka.contrib,
  akka.persistence_cassandra,
  apns,
  akka.testkit % "test",
  spray.testkit % "test"
)

enablePlugins(AkkaAppPackaging)

sbtdocker.Plugin.dockerSettings

docker <<= docker.dependsOn(stage in Compile)

dockerfile in docker <<= (name, stagingDirectory in Universal) map {
  case (appName, stageDir) =>
    val workingDir = s"/lift/$appName"
    new Dockerfile {
      // Use a base image that contain Java
      from("dockerfile/java")
      maintainer("Jan Machacek")
      // Expose REST API on port 8080
      expose(8080)
      // Add the libs dir
      add(stageDir, workingDir)
      run("chmod",  "+x",  s"$workingDir/bin/$appName")
      workDir(workingDir)
      entryPointShell(s"bin/$appName")
    }
}

imageName in docker := {
  ImageName(
    namespace = Some("janm399"),
    repository = "lift",
    tag = Some("v" + version.value))
}
