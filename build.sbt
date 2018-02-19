import Dependencies._
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.{dockerRepository, dockerUpdateLatest}
import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}

enablePlugins(JavaAppPackaging)
enablePlugins(UniversalPlugin)
enablePlugins(DockerPlugin)

name := "school-reader"
organization := "io.sudostream"
scalaVersion := "2.11.8"
version := "0.0.1-13"

//docker
dockerBaseImage := "anapsix/alpine-java:8_server-jre"
dockerRepository := Some("eu.gcr.io/time-to-teach-zone")
dockerUpdateLatest := true
packageName in Docker := "school-reader"

dockerCommands ++= Seq(
  // setting the run script executable
  ExecCmd("RUN",
    "chmod", "u+x",
  //  s"${(defaultLinuxInstallLocation in Docker).value}/bin/${executableScriptName.value}"),
    s"bin/${executableScriptName.value}"),
  // setting a daemon user
  Cmd("USER", "daemon")
)

libraryDependencies ++= {
  val akkaV = "2.5.4"
  val akkaHttpVersion = "10.0.10"
  Seq(
    "io.sudostream.timetoteach" %% "messages" % "0.0.11-54",

    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "com.typesafe.akka" %% "akka-stream" % akkaV,
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-stream-kafka" % "0.17",
    "com.typesafe" % "config" % "1.2.1",

    "com.softwaremill.macwire" %% "macros" % "2.3.0" % "provided",

    "io.netty" % "netty-all" % "4.1.15.Final",
    "org.mongodb.scala" %% "mongo-scala-driver" % "2.1.0",
    "ch.qos.logback" % "logback-classic" % "1.1.7",

    // test
    "org.mockito" % "mockito-all" % "1.10.19" % "test",
    scalaTest % Test
  )
}

fork in run := true

javaOptions in run ++= Seq(
  "-Djavax.net.ssl.keyStore=/etc/ssl/cacerts",
  "-Djavax.net.ssl.keyStorePassword=the8balL",
  "-Djavax.net.ssl.trustStore=/etc/ssl/cacerts",
  "-Djavax.net.ssl.trustStorePassword=the8balL"
)


