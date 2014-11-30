import sbt._
import Keys._
import Dependencies._

object Project {

  val settings = Defaults.defaultConfigs ++ Seq(
    organization := "cakesolutions",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala,
    scalacOptions ++= Seq(
      "-encoding", "UTF-8",
      "-target:jvm-1.7",
      "-deprecation",
      "-unchecked",
      "-Ywarn-dead-code",
      "-feature"
    ),
    javacOptions ++= Seq(
      "-source", "1.7",
      "-target", "1.7",
      "-Xlint:unchecked",
      "-Xlint:deprecation"
    ),
    javaOptions ++= Seq(
      "-Xmx2G"
    )
  )

}
