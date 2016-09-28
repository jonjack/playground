sbtPlugin := true

name := "play-cxf-wsdl2java"

organization := "io.sitemetric.sbt.plugins"

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.4"

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

// disable publishing the main API jar
publishArtifact in (Compile, packageDoc) := false

// disable publishing the main sources jar
publishArtifact in (Compile, packageSrc) := false

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := (
  <url>https://github.com/sitemetric/play-cxf-wsdl2java</url>
  <licenses>
    <license>
      <name>MIT</name>
      <url>http://opensource.org/licenses/mit-license.php</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:sitemetric/play-cxf-wsdl2java.git</url>
    <connection>scm:git:git@github.com:sitemetric/play-cxf-wsdl2java.git</connection>
  </scm>
  <developers>
    <developer>
      <id>jonjack</id>
      <name>Jon Jackson</name>
      <url>http://www.sitemetric.io</url>
    </developer>
  </developers>)