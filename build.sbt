import sbt._
import Keys._

name := "HelloWorld"

lazy val logging = project.in(file("lib/logging"))

lazy val cluster = project.in(file("lib/cluster")).dependsOn(logging)

lazy val main = project.in(file("main")).dependsOn(cluster, logging)

lazy val root = project.in(file(".")).aggregate(main, cluster, logging)
