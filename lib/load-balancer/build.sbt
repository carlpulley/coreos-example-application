import Dependencies._

Project.settings

name := "load-balancer"

libraryDependencies ++= Seq(
  // Miscellaneous
  commons,
  etcd,
  logback,
  typesafe
)
