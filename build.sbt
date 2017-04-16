name := "EPFLBot"
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

resolvers += Resolver.sonatypeRepo("releases")
resolvers += Resolver.sonatypeRepo("snapshots")
resolvers += Resolver.typesafeIvyRepo("releases")

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

libraryDependencies ++= Seq(
  "info.mukel"                 %% "telegrambot4s"           % "2.2.1-SNAPSHOT",
  "net.ruippeixotog"           %% "scala-scraper"           % "1.2.0",
  "com.github.nscala-time"     %% "nscala-time"             % "2.16.0",
  "com.sksamuel.elastic4s"     %% "elastic4s-core"          % "5.2.11",
  "com.sksamuel.elastic4s"     %% "elastic4s-tcp"           % "5.2.11",
  "com.sksamuel.elastic4s"     %% "elastic4s-circe"         % "5.2.11",
  "com.lightbend"              %% "emoji"                   % "1.1.1",
  "com.iheart"                 %% "ficus"                   % "1.4.0",
  "com.vividsolutions"         % "jts"                      % "1.13",
  "org.locationtech.spatial4j" % "spatial4j"                % "0.6",
  "ch.qos.logback"             % "logback-classic"          % "1.1.6",
  "ch.qos.logback"             % "logback-access"           % "1.1.6",
  "net.logstash.logback"       % "logstash-logback-encoder" % "4.8",
  "org.apache.logging.log4j"   % "log4j-to-slf4j"           % "2.8.1"
)

fork := true
cancelable in Global := true

test in assembly := {}
target in assembly := file("dock")
assemblyJarName in assembly := "epflbot.jar"
mainClass in assembly := Some("ch.epfl.telegram.InlineEpflBot")
assemblyMergeStrategy in assembly := {
  case "META-INF/io.netty.versions.properties"       => MergeStrategy.first
  case "META-INF/log4j-provider.properties"          => MergeStrategy.first
  case x if x.startsWith("org/apache/logging/log4j") => MergeStrategy.first
  case x if x.startsWith("io/netty")                 => MergeStrategy.first
  case x                                             => (assemblyMergeStrategy in assembly).value(x)
}
