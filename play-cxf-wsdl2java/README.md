play-cxf-wsdl2java
=================

An [SBT](http://www.scala-sbt.org/) plugin for generating a Java model from WSDL(s) using CXF's [wsdl2java](http://cxf.apache.org/docs/wsdl-to-java.html) tool.

## Requirements

* [SBT 0.13+](http://www.scala-sbt.org/)


## Quick start

Add plugin to *project/plugins.sbt*:

```scala

resolvers += "Sonatype Repository" at "https://oss.sonatype.org/content/groups/public"

addSbtPlugin("io.sitemetric.sbt.plugins" % "play-cxf-wsdl2java" % "1.0-SNAPSHOT")
```

For *.sbt* build definitions, inject the plugin settings in *build.sbt*:

```scala
seq(cxf.settings :_*)
```

For *.scala* build definitions, inject the plugin settings in *Build.scala*:

```scala
Project(..., settings = Project.defaultSettings ++ io.sitemetric.sbt.plugins.Wsdl2JavaPlugin.cxf.settings)
```

## Configuration

Plugin keys are located in `io.sitemetric.sbt.plugins.Wsdl2JavaPlugin.Keys`

### WSDL's

If you are locating your WSDL's in anywhere other than your compile phase resource directory then you need to amend the path below or reconfigure the value of ```resourceDirectory``` outside of this config.

```scala

cxf.wsdls := Seq(
      cxf.Wsdl((resourceDirectory in Compile).value / "Service.wsdl", Seq("-p",  wsclientPackage), "unique wsdl id"),
      ...
)
```
### Packaging

You can define a single common package for the generated artifacts but this will squash the packaging defined in the WSDL so if you wish to maintain a the packagin defined by the WSDL then omit the following:

```scala

lazy val wsclientPackage := "com.company"
```

## Commands

```~wsdl2java``` To automatically generate source code when a WSDL is changed

