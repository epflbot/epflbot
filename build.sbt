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

libraryDependencies ++= Seq(
  "com.github.mukel" %% "telegrambot4s" % "v1.2.2",
  "net.ruippeixotog" %% "scala-scraper" % "1.1.0",
  "com.github.nscala-time" %% "nscala-time" % "2.14.0"
)
