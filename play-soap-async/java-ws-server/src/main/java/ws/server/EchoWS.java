package ws.server;

import java.io.IOException;

import javax.jws.WebService;
import javax.xml.ws.Endpoint;


@WebService(serviceName = "EchoService", targetNamespace = "http://www.site.org")
public class EchoWS {

	public String echo(String request) {

		long threadId = Thread.currentThread().getId();
		System.out.println("WS Service thread: " + threadId);
		
		// ********************************
		long timer = 0; 					// millisecs for service Thread to sleep
		// ********************************
		
		try {
			System.out.println("WS SERVER Sleeping for " + timer + " milliseconds");
			Thread.sleep(timer);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		
		return "Request message '" + request + "' echoed by remote WS Server.";
	}

	public static void main(String[] args) throws IOException {
		String url = args.length > 0 ? args[0] : "http://localhost:8080/ws/echo";
		Endpoint ePoint = Endpoint.publish(url, new EchoWS());
		System.out.println("\r\n**************************************************************************");
		System.out.println("SERVER: Web service endpoint published at: " + url);
		System.out.println("This service simply echoes the request message back to the client.");
		System.out.println("**************************************************************************\r\n");
		System.in.read();
		//ePoint.stop();
	}
	
}

