package controllers

import java.io.File
import java.nio.file.attribute.PosixFilePermission._
import java.nio.file.attribute.PosixFilePermissions
import java.nio.file.{Files, Path}
import java.util
import javax.inject._

import akka.stream.IOResult
import akka.stream.scaladsl._
import akka.util.ByteString
import play.api._
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.streams._
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc._
import play.core.parsers.Multipart.FileInfo

import scala.concurrent.Future
import java.nio.file.StandardCopyOption._
import java.nio.file.Paths

case class FormData(filename: String)

/**
 * Largely copied from Play frameworks own sample file upload implementation.
 */
@Singleton
class UploadController @Inject() (implicit val messagesApi: MessagesApi, conf: Configuration) extends Controller with i18n.I18nSupport {

  private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)

  /** Return a simple upload form. */
  def index = Action { implicit request =>
    Ok(views.html.index(form))
  }

  val form = Form(
      mapping("filename" -> text)(FormData.apply)(FormData.unapply))

  /**
   * Trash the original temporary file after we have done with it.
   */
  private def delete(file: File) = {
    //logger.info(s"deleting temporary file = ${file.getPath}")
    Files.deleteIfExists(file.toPath)
    "DELETED"
  }

 /**
   * Copy a file to our application-specific location, overwriting it if it already exists there.
   * 
   * TODO
   * 
   * - this is where we need to call any manipulation operations - resizing etc - to create 
   *   possible multiple variations of the original image file
   * - we can control the application-local path where we drop the new copies to be dynamic ie. based on user, or catch, id ?
   * 
   */
  private def copyFile(file: File, name: String) = {
    val fileStoreBasePath = conf.underlying.getString("image.store")    // file store path from config
    val path = Files.copy(file.toPath(), Paths.get(fileStoreBasePath, ("copy_" + name)), REPLACE_EXISTING)
    path
  }
  
  /** 
   *  Type of multipart file handler to be used by body parser 
   */    
  type FilePartHandler[A] = FileInfo => Accumulator[ByteString, FilePart[A]]

  /**
   * A FilePartHandler which returns a File, rather than Play's TemporaryFile class.
   */
  private def handleFilePartAsFile: FilePartHandler[File] = {
    
    case FileInfo(partName, filename, contentType) =>
      val attr = PosixFilePermissions.asFileAttribute(util.EnumSet.of(OWNER_READ, OWNER_WRITE))
      val path: Path = Files.createTempFile("multipartBody", "tempFile", attr)
      val file = path.toFile
      val fileSink: Sink[ByteString, Future[IOResult]] = FileIO.toPath(file.toPath())
      val accumulator: Accumulator[ByteString, IOResult] = Accumulator(fileSink)
      
      accumulator.map {
        case IOResult(count, status) =>
          FilePart(partName, filename, contentType, file)
      } (play.api.libs.concurrent.Execution.defaultContext)
  }

  /**
   * Action to manage multipart file upload as form POST.
   */
  def upload = Action(parse.multipartFormData(handleFilePartAsFile)) { implicit request =>
    
    val fileOption = request.body.file("filename").map {
      
      case FilePart(key, filename, contentType, file) =>
        val copy = copyFile(file, filename)
        val deleted = delete(file)  // delete original uploaded file after we have 
        copy   
    }
    Ok(s"file size = ${fileOption}")
  }

}
