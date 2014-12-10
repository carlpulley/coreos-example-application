import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys._
import sbt._
import Keys._

name := "HelloWorld"

lazy val common = project.in(file("lib/common"))

lazy val logging = project.in(file("lib/logging")).dependsOn(common)

lazy val etcd = project.in(file("lib/etcd")).dependsOn(common)

lazy val api = project.in(file("lib/api")).dependsOn(common, logging)

lazy val cluster = project.in(file("lib/cluster")).dependsOn(common, etcd, logging) configs (MultiJvm)

lazy val main = project.in(file("main")).dependsOn(api, cluster, common, etcd, logging)

lazy val root = project.in(file(".")).aggregate(api, cluster, common, etcd, logging, main)
