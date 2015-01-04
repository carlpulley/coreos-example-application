import sbt._

object Resolvers {

  lazy val resolvers = Seq(
    Resolver.defaultLocal,
    Resolver.mavenLocal,
    Resolver.sonatypeRepo("releases"),
    Resolver.typesafeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  )

}
