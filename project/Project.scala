import sbt._
import Keys._
import com.typesafe.sbt.SbtMultiJvm
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.MultiJvm
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

  val clusterSettings = settings ++ SbtMultiJvm.multiJvmSettings ++ Seq(
    // make sure that MultiJvm test are compiled by the default test compilation
    compile in MultiJvm <<= (compile in MultiJvm) triggeredBy (compile in Test),
    // disable parallel tests
    parallelExecution in Test := false,
    // make sure that MultiJvm tests are executed by the default test target,
    // and combine the results from ordinary test and multi-jvm tests
    executeTests in Test <<= (executeTests in Test, executeTests in MultiJvm) map {
      case (testResults, multiNodeResults)  =>
        val overall =
          if (testResults.overall.id < multiNodeResults.overall.id)
            multiNodeResults.overall
          else
            testResults.overall
        Tests.Output(overall,
          testResults.events ++ multiNodeResults.events,
          testResults.summaries ++ multiNodeResults.summaries)
    }
  )

}
