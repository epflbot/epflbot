name := "Apety-server"
version := "0.1.0"

scalaVersion := "2.11.8"
scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint:_"
)

resolvers += "jitpack" at "https://jitpack.io"
resolvers += Resolver.sonatypeRepo("releases")

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

libraryDependencies += "com.github.mukel" %% "telegrambot4s" % "v2.0.1" exclude("org.slf4j", "slf4j-log4j12")

libraryDependencies ++= Seq(
  "net.ruippeixotog" %% "scala-scraper" % "1.1.0",
  "com.github.nscala-time" %% "nscala-time" % "2.14.0",
  "com.sksamuel.elastic4s" %% "elastic4s-core" % "5.0.0",
  "com.sksamuel.elastic4s" %% "elastic4s-circe" % "5.0.0",
  "com.vividsolutions" % "jts" % "1.13",
  "org.locationtech.spatial4j" % "spatial4j" % "0.6",
  "org.apache.logging.log4j" % "log4j-core" % "2.7",
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.7"
)
