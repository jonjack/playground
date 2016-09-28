package controllers
import java.util.concurrent.ExecutionException

import scala.concurrent.Future
import scala.concurrent.Promise
import scala.util.Try

import async.client.AddNumbersResponse
import async.client.AddNumbersService
import javax.xml.ws.AsyncHandler
import javax.xml.ws.{ Response => JaxwsResponse }
import play.api._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._

object AsyncSoapController extends Controller {

  
  /*
   * The add function takes 2 Int arguments - a & b
   * 
   * I DONT GET THE ACTION METHOD SYNTAX - CLASS { } 
   * THEY SEEM TO RETURN ACTIONS WHEN I THOUGHT THEY SHOULD RETURN A RESULT
   * 
   * public final play.api.mvc.Action async(scala.Function0 block);
   * Action.async seems like a method that takes a function as an argument
   * 
   */
  def add(a: Int, b: Int) = Action.async {
    
      // we pass the arguments to addUsingWebService method which returns a Future[Either[String, Int]] 
      //val futureInt = scala.concurrent.Future { addUsingWebService(a, b) }
      
      Logger.info("--------------------------------------------------------------------")
      
          
      val threadId = Thread.currentThread().getId();
      val hash = System.identityHashCode();
      Logger.info("#1 SOAP CLIENT " + hash + " ACTION Running in thread:         " + threadId)
      
      
      // Not sure why we need scala.concurrent.Future part
      val futureInt = addUsingWebService(a, b)
      
     
      
      // map takes a function and applies it to each element of futureInt
      // function: i => Ok("Got result: " + i)
      
      // I DONT UNDERSTAND WHERE WE ARE GETTING 'i' from ?????
      
      futureInt.map(i => Ok("Got result: " + i))
      //futureInt onSuccess {
        //case msg => println(msg)
      //}
  }
  
  //def add(a: Int, b: Int) = Action {
    //AsyncResult {
      //addUsingWebService(a, b) map {
      //  case Right(sum) => Ok(s"And the sum is: $sum")
        //case Left(errMsg) => InternalServerError(s"Ooops, $errMsg")
      //}

    //}
  //}

  private def addUsingWebService(a: Int, b: Int): Future[Either[String, Int]] = {
    
    // watch out: JAXWS service constructors access the (possibly remote) WSDL a create the service delegate
    // this will cause a blocking HTTP GET
    // the service instance could be shared by all requests
    //  - maybe created on application startup
    //	- the WSDL could be stored locally to prevent dependency on the web service's availability at startup time
    // thread-safety of the port depends on the JAXWS implementation
    
    
    // AddNumbersService
    val svc = new AddNumbersService
    
    
    // Get the proxy object (port) from the AddNumbersService
    // This proxy object is an instance of AddNumbersImpl
    val port = svc.getAddNumbersImplPort()

    
    // Calls invoke() and passes AddNumbersResponse in as a type parameter
    // This returns a Future
    JaxwsAsyncAdapter.invoke[AddNumbersResponse] { 

      //Logger.info("SOAP CLIENT: Making Async call")
      
      // This calls AddNumbersImpl.addNumbersAsync(a, b, handler)
      handler => port.addNumbersAsync(a, b, handler)
      
    } map { 
      
      //Logger.info("SOAP CLIENT: Got response")
      resp => Right(resp.getReturn)
      
    } recover {
      case e: ExecutionException =>
        Logger.error("Error invoking SOAP web service", e)
        Left(s"Error invoking SOAP web service: ${e.getCause.getMessage}")
    }
  }

}

object JaxwsAsyncAdapter {

  /**
   * Returns a Future containing a result of an asynchronous JAX-WS proxy method invocation.
   * @param invoker function that invokes a JAX-WS proxy method with the specified AsyncHandler
   * @return Future containing the eventual result of JAX-WS proxy method invocation
   */
  def invoke[A](invoker: AsyncHandler[A] => Unit): Future[A] = {
    
    
    // The Promise that will be populated with the response of the Async call
    // Promise[AddNumbersResponse] - so its a 'promise' of an AddNumbersResponse object
    // The get is called inside the AsyncHandler (see below)
    val promise = Promise[A]()
        
    
    
    // The Async Handler function - I think this is what runs in its own thread
    // not sure where the fork occurs though yet ??
    val handler = new AsyncHandler[A] {
      
      val threadId = Thread.currentThread().getId();
        val hash = System.identityHashCode();
        Logger.info("#2 SOAP CLIENT " + hash + " Inside AsyncHandler declaration in thread:   " + threadId)
      
      override def handleResponse(response: JaxwsResponse[A]) = {
        // javax.xml.ws.Response is a java.util.concurrent.Future implementation
        // that might throw an exception (like java.util.concurrent.ExecutionException)
        // when the computation result is retrieved (response.get)
        
        val threadId = Thread.currentThread().getId();
        val hash = System.identityHashCode();
        Logger.info("#3 SOAP CLIENT " + hash + " ASYNCHANDLER Running in thread:   " + threadId)
        
        //Logger.info("SOAP CLIENT: PROMISE COMPLETE: " + promise.isCompleted)
        
        
        // This is where we get the Response - which thread does it run in ???
        promise.complete(Try(response.get))
       
      }
    }
    
    
    val threadId3 = Thread.currentThread().getId();
    Logger.info("#4 SOAP CLIENT Invoking AsyncHandler in thread:   " + threadId3)
        
    // The invoker is a AsyncHandler[A] or specifically AsyncHandler[AddNumbersResponse]
    // we are passing in the 'handler' which is an instance of AsyncHandler[A] declared above
    // So javax.xml.ws.AsyncHandler[AddNumbersResponse] is what handles the response for us in another thread.
    // i think this must be where the new thread is spawned
    invoker(handler)     
    
    // this now returns the Future (contained in the Promise) so that the Action can return immediately
    // the AsyncHandler is still running in the other thread waiting to complete
    // Q. So the AsyncHandler.handleResponse function (overidden above) gets passed a JaxwsResponse (by JAX I assume)
    // and the Promise is completed by doing a response.get
    // Q. how does handleResponse() gt called?
    // Q. I understand tht a copy of the Promise.Future is returned by the Action and then completed by the AsyncHandler
    // in another thread. but then how does Play know how to send the response back?
    promise.future      
  }

}