package io.sitemetric.sbt.plugins

import sbt._
import sbt.Keys._
import java.io.File
import scala.language.postfixOps

/*
 * @author      Jon Jackson
 */
object Wsdl2JavaPlugin extends Plugin {

  trait Keys {

    lazy val Config = config("cxf") extend(Compile) hide
    lazy val cxfVersion = settingKey[String]("cxf version")
    lazy val wsdl2java = taskKey[Seq[File]]("Generates java files from wsdls")
    lazy val wsdls = settingKey[Seq[cxf.Wsdl]]("wsdls to generate java files from")

  }  

  // TODO: Allow the client to pass the version into the plugin
  private object CxfDefaults extends Keys {
    val settings = Seq(
      cxfVersion := "3.0.4",
      libraryDependencies ++= Seq[ModuleID](
        "org.apache.cxf" % "cxf-tools-wsdlto-core" % cxfVersion.value % Config.name,
        "org.apache.cxf" % "cxf-tools-wsdlto-databinding-jaxb" % cxfVersion.value % Config.name,
        "org.apache.cxf" % "cxf-tools-wsdlto-frontend-jaxws" % cxfVersion.value % Config.name
      ),
      wsdls := Nil
    )
  }

  object cxf extends Keys {

    // TODO: 
    // each entry added to the cxf.wsdls Seq in build.sbt creates an instance of the Wsdl case class
    // this has 3 params:
    // 1. the WSDL file path
    // 2. the Seq of args that the user of the plugin can provide
    // 3. the key which is the name of the directory under which the artifacts get generated
    case class Wsdl(file: File, args: Seq[String], key: String) {
      def outputDirectory(basedir: File) = new File(basedir, key).getAbsoluteFile
    }

    val settings = Seq(ivyConfigurations += Config) ++ CxfDefaults.settings ++ Seq(
         
      sourceManaged in Config := sourceManaged.value / "cxf",
      managedSourceDirectories in Compile += {(sourceManaged in Config).value}, // adds cxf output folder to list of source dirs to compile
      managedClasspath in wsdl2java <<= (classpathTypes in wsdl2java, update) map { (ct, report) =>
          Classpaths.managedJars(Config, ct, report)
      },

      wsdl2java := {
        val s: TaskStreams = streams.value
        val classpath : String = (((managedClasspath in wsdl2java).value).files).map(_.getAbsolutePath).mkString(System.getProperty("path.separator"))
       
        
        // TODO: Generate artifacts to app/models/<output folder> since this is why we have renamed project as a Play Plugin     
        val basedir : File = target.value / "cxf"     // generates baseDirectory/target/cxf
        //val basedir : File = baseDirectory.value / "app/models/sap"      // generates baseDirectory/app/models/sap
        
      
        IO.createDirectory(basedir)  //creates target/cxf
        
        // wsdl's is the sequence of filenames defined in plugin config in build.sbt 
        wsdls.value.par.foreach { wsdl =>
          
          val output : File = wsdl.outputDirectory(basedir)   // creates target/cxf/<key> for each WSDL
          
          // compares last time the WSDL was modified to last time directory was created
          if(wsdl.file.lastModified() > output.lastModified()) { 
            
            val id : String = wsdl.key
            val args : Seq[String] = Seq("-d", output.getAbsolutePath, "-verbose", "-autoNameResolution", "-exsh", "true", "-fe", "jaxws21") ++ wsdl.args :+ wsdl.file.getAbsolutePath

            
            // TODO: Logging is repeated for each and every WSDL at present
            // Introduce some logic so that we only log our build events once per output folder
            // ie. for each iteration, add each output folder into a Sequence (if it doesnt already exist there) 
            // and then ignore logging if that output folder has already been 'removed/created' etc
            
            s.log.info("Removing output directory for " + id + " ...")
            IO.delete(output)
            
            s.log.info("Compiling " + id)
            val cmd = Seq("java", "-cp", classpath, "-Dfile.encoding=UTF-8", "org.apache.cxf.tools.wsdlto.WSDLToJava") ++ args
            
            //s.log.info(cmd.toString()) // This will output whole classpath 
            
            cmd ! s.log
            
            s.log.info("Finished " + id)
            
          } else {
            
            s.log.debug("Skipping " + wsdl.key)
            
          }
          
          IO.copyDirectory(output, (sourceManaged in Config).value, true)
        }
        
        ((sourceManaged in Config).value ** "*.java").get
      },
      
      sourceGenerators in Compile <+= wsdl2java)
  }
}
