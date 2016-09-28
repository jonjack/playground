name := "ws-server"

version := "1.0-SNAPSHOT"

autoScalaLibrary := false

crossPaths := false

EclipseKeys.projectFlavor := EclipseProjectFlavor.Java

fork in run := true

javaOptions in (run) += "-Djdk.logging.allowStackWalkSearch=true"