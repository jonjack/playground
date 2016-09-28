package cz.mencik.ws.server;

import java.io.IOException;

import javax.jws.WebService;
import javax.xml.ws.Endpoint;

@WebService(serviceName = "AddNumbersService", targetNamespace = "http://duke.example.org")
public class AddNumbersImpl {

	public int addNumbers(int number1, int number2) {
		// throw new UnsupportedOperationException();
		
		long hash = System.identityHashCode(this);
		System.out.println("SOAP SERVER:  " + hash + "   Request: " + number1 + " + " + number2 + " = " + (number1 + number2));
		
		
		// The longer the thread goes to sleep - the longer the client waits for its response.
		// By making this longer we will hopefully see the request handler returned to the request handling Pool and
		// a new thread handling the callback from this thread
		
		// I was trying to figure out why - if you make several requests in quick succession - the Async threads
		// all seem to wait for 'timeout' seconds and then respond all at once.
		// This is because each different request for this Server's addNumber() method runs in its own thread.
		// Because all the requests come in quick succession, a new thread is assigned to each request and they all
		// wait around for 7 seconds and then return their result of number1 + number2
		// ie. this object is not shared (each thread gets its own AddNumbersImpl) and they therefore all run 
		// their 'copy' of addNumbers() in parallel.
		// Even if AddNumbersImpl was a shared object, this method is not protected by any locks so each thread would 
		// just run through addNumber() in parallel anyway
		
		// ********************************
		long timer = 0; 					// millisecs
		// ********************************
		System.out.println("SOAP SERVER: Sleeping for " + timer + " seconds");
		
		try {
			System.out.println("SOAP SERVER: Sleeping ZZZZZ");
			Thread.sleep(timer);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		return number1 + number2;
	}

	public static void main(String[] args) throws IOException {
		String url = args.length > 0 ? args[0] : "http://localhost:8080/ws-server/AddNumbers";
		Endpoint ep = Endpoint.publish(url, new AddNumbersImpl());
		System.out.println("SOAP SERVER: Web service endpoint published at: " + url);
		System.out.println("SOAP SERVER: Press Enter key to stop the web service endpoint");
		System.in.read();
		//ep.stop();
	}
	
}

