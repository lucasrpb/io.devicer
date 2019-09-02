import Dependencies._

name := "io.devicer"

version := "0.1"

scalaVersion := "2.12.9"

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots"),
  DefaultMavenRepository
)

val netty4Version = "4.1.35.Final"

libraryDependencies ++= Seq(
  "org.scalactic" %% "scalactic" % "3.0.5",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "com.google.guava" % "guava" % "27.1-jre",
  "org.apache.commons" % "commons-lang3" % "3.8.1",

  "org.scala-lang.modules" %% "scala-collection-compat" % "2.0.0",

  "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",

  Library.vertx_codegen,
  Library.vertx_lang_scala,
  Library.vertx_hazelcast,
  Library.vertx_kafka_client,
  "io.vertx" % "vertx-mqtt-scala_2.12" % "3.8.0",

  "com.twitter" %% "finagle-http" % "19.5.1",
  "com.twitter" %% "finagle-core" % "19.5.1",
  "io.netty" % "netty-all" % netty4Version
)

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)