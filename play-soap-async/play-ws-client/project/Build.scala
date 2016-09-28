import sbt._
import Keys._
import play.Play.autoImport._

object ApplicationBuild extends Build {

  val appName = "play-ws-client"
  val scalaVersion = "2.11.6"
  val appVersion = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "org.apache.cxf" % "cxf-rt-frontend-jaxws" % "3.0.4",
    "org.apache.cxf" % "cxf-rt-transports-http-hc" % "3.0.4"
  )

  val main = Project(appName, file(".")).enablePlugins(play.PlayScala).settings(
    version := appVersion,
    libraryDependencies ++= appDependencies
  )

}
