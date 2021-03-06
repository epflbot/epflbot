import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import sbt.Package.ManifestAttributes

name := "EPFLBot"
version := "0.3.0"

scalaVersion := "2.12.6"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint:_"
)

resolvers += Resolver.sonatypeRepo("releases")
resolvers += Resolver.sonatypeRepo("snapshots")
resolvers += Resolver.typesafeIvyRepo("releases")

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)

libraryDependencies ++= {
  val elasticV = "5.4.15"

  Seq(
    "info.mukel"                 %% "telegrambot4s"                 % "2.9.5",
    "net.ruippeixotog"           %% "scala-scraper"                 % "1.2.1",
    "com.github.nscala-time"     %% "nscala-time"                   % "2.16.0",
    "com.sksamuel.elastic4s"     %% "elastic4s-core"                % elasticV,
    "com.sksamuel.elastic4s"     %% "elastic4s-tcp"                 % elasticV,
    "com.sksamuel.elastic4s"     %% "elastic4s-circe"               % elasticV,
    "com.lightbend"              %% "emoji"                         % "1.2.0",
    "com.iheart"                 %% "ficus"                         % "1.4.3",
    "com.vividsolutions"         % "jts"                            % "1.13",
    "org.locationtech.spatial4j" % "spatial4j"                      % "0.6",
    "ch.qos.logback"             % "logback-classic"                % "1.2.3",
    "ch.qos.logback"             % "logback-access"                 % "1.2.3",
    "com.internetitem"           % "logback-elasticsearch-appender" % "1.6",
    "org.apache.logging.log4j"   % "log4j-to-slf4j"                 % "2.11.0",
    "com.unboundid"              % "unboundid-ldapsdk"              % "3.2.1"
  )
}

fork := true
cancelable in Global := true
parallelExecution in Test := false

assemblyJarName in assembly := "epflbot.jar"
test in assembly := {}
target in assembly := file("build")
mainClass in assembly := Some("ch.epfl.telegram.EpflBot")
packageOptions := List(
  ManifestAttributes(
    "Change"     -> version.value,
    "Build-Date" -> LocalDateTime.now.format(DateTimeFormatter.ISO_DATE_TIME)
  )
)
assemblyMergeStrategy in assembly := {
  case "META-INF/io.netty.versions.properties"       => MergeStrategy.first
  case "META-INF/log4j-provider.properties"          => MergeStrategy.first
  case x if x.startsWith("org/apache/logging/log4j") => MergeStrategy.first
  case x if x.startsWith("io/netty")                 => MergeStrategy.first
  case x                                             => (assemblyMergeStrategy in assembly).value(x)
}

enablePlugins(BuildInfoPlugin)
buildInfoPackage := "BuildInfo"
buildInfoPackage := "ch.epfl.telegram"
buildInfoKeys := List[BuildInfoKey](version)
