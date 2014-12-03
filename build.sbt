import sbt._
import Keys._

name := "HelloWorld"

lazy val logging = project.in(file("lib/logging"))

lazy val cluster = project.in(file("lib/cluster")).dependsOn(logging)

lazy val persistence = project.in(file("lib/persistence")).dependsOn(logging)

lazy val main = project.in(file("main")).dependsOn(cluster, logging, persistence)

lazy val root = project.in(file(".")).aggregate(main, cluster, logging, persistence)
