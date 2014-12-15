import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys._
import sbt._
import Keys._

name := "Lift"

lazy val common = project.in(file("lib/common"))

lazy val logging = project.in(file("lib/logging")).dependsOn(common)

lazy val etcd = project.in(file("lib/etcd")).dependsOn(common, logging)

lazy val api = project.in(file("lib/api")).dependsOn(common, logging)

lazy val cluster = project.in(file("lib/cluster")).dependsOn(common, etcd, logging) configs (MultiJvm)

lazy val persistence = project.in(file("lib/persistence")).dependsOn(common, logging)

// 'Lift' application
//Common code, but not protocols
lazy val lift_common = project.in(file("lift/common"))

//Exercise
lazy val exercise = project.in(file("lift/exercise")).dependsOn(api, cluster, common, etcd, logging, persistence, notificationProtocol, profileProtocol, lift_common)

//User profiles
lazy val profile = project.in(file("lift/profile")).dependsOn(api, cluster, common, etcd, logging, persistence, profileProtocol, lift_common)
lazy val profileProtocol = project.in(file("lift/profile-protocol")).dependsOn(lift_common)

//Notifications
lazy val notification = project.in(file("lift/notification")).dependsOn(cluster, lift_common, notificationProtocol, profileProtocol)
lazy val notificationProtocol = project.in(file("lift/notification-protocol")).dependsOn(lift_common)

lazy val lift = project.in(file(".")).aggregate(api, cluster, common, etcd, logging, persistence, lift_common, exercise, profile, profileProtocol, notification, notificationProtocol)
