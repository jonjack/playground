package controllers

import java.util.concurrent.ExecutionException
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.util.Try
import javax.xml.ws.AsyncHandler
import javax.xml.ws.{ Response => JaxwsResponse }
import play.api._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._
import org.site.EchoService
import org.site.EchoResponse
import scala.util.Random

/* Example Async SOAP client.
 * --------------------------
 * If there is other orchestration to be done with the result before returning the 
 * response then you probably wouldn't want to do this from a controller.
 */
object AsyncSoapController extends Controller {

  /* 
   * Action that returns a Future of a Result.
   */
  def echo(req: String) = Action.async {
      val futureInt = echoServiceCaller(req)
      futureInt.map(i => Ok("Got result: " + i))
  }
  
  private def echoServiceCaller(req: String): Future[Either[String, String]] = {
    
    val svc = new EchoService            // JAX-WS Service Implementation
    val port = svc.getEchoWSPort()

    JaxWSAsync.invoke[EchoResponse] { 
      handler => port.echoAsync(req, handler)      
    } map { 
      resp => Right(resp.getReturn)     
    } recover {
      case e: ExecutionException =>
        Logger.error("Error invoking SOAP web service", e)
        Left(s"Error invoking SOAP web service: ${e.getCause.getMessage}")
    }

  }

}

/*
 * A generic async handler.
 */
object JaxWSAsync {

  def invoke[A](invoker: AsyncHandler[A] => Unit): Future[A] = {

    val promise = Promise[A]()
    
    /* Defines the JAX-WS Handler Implementation */
    val handler = new AsyncHandler[A] {
      
        val randomID = Random.nextInt(9999)   
        val threadID = Thread.currentThread().getId();
        Logger.info(randomID + " Action in Thread: " + threadID)
      
        
      /********************************************************************** 
       * The Callback that will be invoked in another place at another time *
       **********************************************************************/ 
      override def handleResponse(response: JaxwsResponse[A]) = { 
        val randomID2 = Random.nextInt(9999)
        val threadID2 = Thread.currentThread().getId();
        Logger.info(randomID2 + " AsyncHandler in thread:   " + threadID2)
        promise.complete(Try(response.get))
       
      }
        
    }
    
    invoker(handler)    // execute 'invoke' and then 
    promise.future      // return the Future of the Promise
  }
  

}
