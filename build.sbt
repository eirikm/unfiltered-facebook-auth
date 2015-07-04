organization := "com.example"

name := "unfiltered-facebook-auth"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.11.7"

val unfilteredVersion = "0.8.4"

libraryDependencies ++= Seq(
  "io.argonaut" %% "argonaut" % "6.0.4",
  "com.typesafe" % "config" % "1.3.0",
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.2",
  "net.databinder" %% "unfiltered-directives" % unfilteredVersion,
  "net.databinder" %% "unfiltered-filter" % unfilteredVersion,
  "net.databinder" %% "unfiltered-jetty" % unfilteredVersion,
  "net.databinder" %% "unfiltered-specs2" % unfilteredVersion % "test",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)

resolvers ++= Seq(
  "java m2" at "http://download.java.net/maven/2"
)
