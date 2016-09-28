
#Play - Async/Non Blocking - SOAP Client/Server

---
This is a proof of concept to research making Asynchronous non-blocking web service calls to a SOAP Endpoint.

---
###TODO:

- have the WSDL configured locally otherwise there is a potential blocking get for the WSDL exposed by the service
- implement a blocking implementation of the AsyncController so that we can draw some comparisons with benchmarking tools between the Blocking and Non-Blocking approach
- Write up some recommendations for running realistic experiments based on a consideration that if the client / server / benchmark tool all run on the same machine then they will contend for the available cores, increasing context switching etc.

---

###Running the Play Client

This is a simple [Play Framework](http://www.playframework.com/) project that consumes a simple SOAP _echo_ web service.

Run the client application with Typesafe's Activator:
```
play-ws-client$ activator run
```

The client application runs on Play's default Netty port `9000` - [http://localhost:9000/](http://localhost:9000/)

The client has a single action `echo` which takes a single query parameter `req` which represents the message you want to send to the echo SOAP web service.

A valid request would look like [http://localhost:9000/echo?req=Hello](http://localhost:9000/echo?req=Hello)

---

###Running the Server

The server is a simple SBT project so you can run it using SBT, or Activator (if you have Typesafe's Activator installed):
```
server$ activator run

server$ sbt run

```

---

###Creating a WSDL

The server project exposes a javax.xml.ws.Endpoint as follows:

```
...
import javax.jws.WebService;   // Web Services Metadata For The Java Platform API
import javax.xml.ws.Endpoint;  // rt.jar (SDK)
...
public static void main(String[] args) throws IOException {
		String url = args.length > 0 ? args[0] : "http://localhost:8080/ws/echo";
		Endpoint ep = Endpoint.publish(url, new EchoImpl());
...
```
This is all that is required to browse a dynamically generated WDSL at: `http://localhost:8080/ws/echo?wsdl`.

Make a document out of this published WSDL and put it somewhere. You can now use a tool such as CXF's wsdl2java to generate a set of starting point code as outlined here `http://cxf.apache.org/docs/wsdl-to-java.html`.

If the WSDL is in an `ECHO` dir at CXF/bin, then you can run the following command in a terminal window whilst being located in CXF/bin:

```
CXF/bin> ./wsdl2java -all -frontend jaxws21 -asyncMethods=echo -d ECHO ECHO/Echo.wsdl
```
Note that you will need to have the server running because the generated WSDL describes the XSD (that wsdl2java needs to use) to be located at `schemaLocation="http://localhost:8080/ws/echo?xsd=1"`.

---

###Benchmarking

You can use Apache Bench to see how the client / server perform:
```
ab -n 100 -c 10 -l http://localhost:9000/echo?req=Hello
```

Decent benchmarking blog post covering Apache Bench, Siege, and JMeter - http://work.stevegrossi.com/2015/02/07/load-testing-rails-apps-with-apache-bench-siege-and-jmeter/
