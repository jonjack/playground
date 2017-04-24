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

/**
 * Class to model our form data.
 */
case class FormData(filename: String)

/**
 * Largely copied from Play frameworks own sample file upload implementation.
 * 
 * TODO
 * - We need to think about where we file manipulation eg. resizing.
 * 
 */
@Singleton
class UploadController @Inject() (implicit val messagesApi: MessagesApi, conf: Configuration) extends Controller with i18n.I18nSupport {

  private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)

  /**
   * Upload form
   */
  def index = Action { implicit request =>
    Ok(views.html.index(form))
  }
  
  val form = Form(
      mapping("filename" -> text)
      (FormData.apply)
      (FormData.unapply)
    )

  // Trash the original temporary file after we have done with it.
  private def deleteTempFile(file: File) = {
    Files.deleteIfExists(file.toPath)
  }
   
 /**
   *  File copy utility
   *  
   * @file the uploaded file we wish to store 
   * @name the name of the original file
   * 
   * Takes a file object and filename and creates a copy in a path under our control, overwriting it 
   * if it already exists there.
   * 
   * We could use file.getName to get the name but this returns the OS-assigned temporary file name 
   * which if often cryptic and not very useful. The caller is therefore required to pass in the 
   * filename, which can be the original name.
   * 
   * TODO Filestore
   * 
   * - ideally we want a more general solution to manage the store of the uploaded file.
   *   At present we copy it to the local drive here but we should delegate the storage 
   *   function to another component which abstracts away how we store the file.
   *   We should create some sort of generic file store component and allow the user to 
   *   inject the filestore implementation they wish to use.
   *   
   *   Some example filestores:-
   *   - local filesystem
   *   - Amazon S3
   *   - Some other remote location
   * 
   */ 
  private def storeFile(file: File, name: String) = {
    val fileStoreBasePath = conf.underlying.getString("image.store")
    //val path = Files.copy(file.toPath(), Paths.get("/tmp/", ("copy_" + name)), REPLACE_EXISTING)
    val path = Files.copy(file.toPath(), Paths.get(fileStoreBasePath, ("copy_" + name)), REPLACE_EXISTING)
    // using file.getName returns the OS-assigned temporary file name which if often cryptic and not very useful
    //val path = Files.copy(file.toPath(), Paths.get(fileStoreBasePath, ("copy_" + file.getName)), REPLACE_EXISTING)
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
   * 
   * TODO
   * - Need to handle exceptions
   * - Need to implement some file size handling capability
   * 
   */
  def upload = Action(parse.multipartFormData(handleFilePartAsFile)) { implicit request =>
    
    val file = request.body.file("filename").map {
      
      case FilePart(key, filename, contentType, file) =>
        val copy = storeFile(file, filename)
        val deleted = deleteTempFile(file)  // delete original uploaded file after we have 
        copy   
    }
    Ok(s"Uploaded: ${file}")
  }
  

}
