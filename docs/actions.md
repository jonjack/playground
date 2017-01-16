# Controllers & Actions

- [What are Actions?](#)
- [What are Controllers?](#)      
- [The boundary between application and outside world](#)    
- [Actions should be non-blocking](#)    
- [Which thread pool should Actions run in?](#)
- [Action composition](#)
- [Architecture](#)

---

## What are Actions?

...

---

## What are Controllers?

...

---

## The boundary between application and outside world

...

---

## Actions should be non-blocking

...

---

## Which thread pool should Actions run in?

I think that running everything in the default execution context (think thread pool) ie. Actions and all other code, will mean that no matter how well you have carefully coded your actions to be non-blocking, if you do have blocking calls somewhere then your Actions will also be potentially compromised as all threads (those executing blocking and non-blocking code alike) will all be competing for the same set of cores anyway. So I believe the strategy of ensuring Actions only contain non-blocking operations is only effective if you have also taken steps to have blocking code run within a different execution context (ie. a different thread pool). 

Play is designed to be asynchronous and non-blocking everywhere ....

- need to refresh on _asynchronous_ and
- need to refresh on _non-blocking_

Play APIs (eg. WS) are non-blocking in that they do not cause threads to block on a core (context switched). If you have to talk to a DB which is going to block (eg. JDBC) then you have to do this somewhere. I think the advice that Actions should never call any blocking code is not because it will cause the Action to delay in returning (since all Actions are wrapped in a Future they will always be pushed somewhere else to complete asynchronously) but I think it is assumed that to keep your app responsive you are doing any blocking work in code running in a different thread pool somewhere. As I said above, if you are doing things that may block, and using a single thread pool, then I guess you may as well block from an Action since some thread is going to block somewhere and  if you do start to bump up against the number of cores available to that single pool then having ensured your controllers are non-blocking will not save your app from potentially becoming unresponsive - having controllers that are lightning fast doesn't help if there are no spare threads to run them on (even if they do return quickly).

If you do blocking work in another thread pool from that servicing your controllers, then even though clients may be waiting for those blocking calls to complete (from the requests that were passed over to the other pool to complete), your controllers will be able to keep servicing their requests very quickly which means that new clients will still be able to make requests to your application which means it remains responsive. Even though some clients will be kept hanging  around waiting for responses, your application will appear to other clients as being responsive because controllers will still be able to take their requests. 

It is useful to keep in mind that if you are talking to a blocking DB (for example), and your application is running a lot of transactions that causes the DB to queue up requests - eg. lots of heavy reads - then 

---

## Action composition

...

---





        







---

### TLDR

From a logical point of view, Actions are the main gateway through which your Play application manages the requests (from) and responses (to) the clients using the application. From an implementation point of view, an `action` is an object that takes a `Request` object and returns a `Result`. 

---

## Architecture

Here are some diagrams I drew to try and reason about how Actions sit within a Play application architecture.

```scala
                              Your  Play  Application  Code
 Netty HTTP Server     +--------------------+------------------------------+
                       |                    |                              |
   Play Framework      |    Controllers     |                              |
                       |      |             |                              |
                       |      |             |                              |
  HTTP Requests  --------->   |- Action     |                              |
  HTTP Responses <---------   |- Action  ---------► Other Application code |
                       |                    |                              |
                       |          |         |            |                 |
                       +----------|---------+------------|-----------------+
                                  |                      |
                       +----------|----------------------|-----------------+                                                  
                       |          ▼     Web Services     ▼                 |
                       |                 Datastores                        |
                       |                   Caches                          |
                       +---------------------------------------------------+
```



When a request for a resource is made to your Play application, the framework will search the `routes` file to try and match the request to a particular URL pattern. If a match is found, the framework will invoke the method for that pattern and pass it an object representing the `Request`. In return, the framework expects an `Action` object to be returned by the method. This `Action` object is then used to build the `Response` which the framework takes care of passing back to the client.

An example **Mapping** betwen URI and action method

```scala
GET      /home         HomeController.homeActionBuilderMethod
```

An example **Controller** with methods that return an Action to handle the request for `/home`

```scala
class HomeController extends Controller {

  def homeActionBuilderMethod = Action ( request -> response )
  
  // or more commonly (since there is 1 function argument) braces are used 
  def homeActionBuilderMethod = Action { request -> response }
}
```

Each request coming into our application needs to invoke some method which returns an `Action` object appropriate for dealing with that request. We therefore need somewhere to define these methods. This is what Controllers are for.

> #### Controller design pattern
I believe the use of the term _Controller_ (in Play) stems from the _Front Controller_ pattern which is a [structural design pattern](https://en.wikipedia.org/wiki/Software_design_pattern#Structural_patterns) intended to act a single, central entry point to an application. Controllers in Play can generally be thought of more in terms of _Page Controllers_ however (although they are intended to deal with all response types not just _pages_), since they are generally implemented at a more granular level than a Front Controller ie. you implement controllers for different parts of your application rather than having just a single controller.


---

## Actions are the boundary between HTTP and your application 

An `Action` is arguably one of the most fundamental features of the framework, and therefore of any Play application. They form the main boundary (for the application developer in any case) between any Play application code and the HTTP protocol.  


As the Play application developer, you specify the mappings between the URL's that your application will serve results for, and the actions that build those results, in a `routes` file. The following example mapping will cause any HTTP request using the GET method on the `home` resource, to invoke the `homeActionBuilderMethod` (contained in the `HomeController`).

```scala
GET      /home         HomeController.homeActionBuilderMethod
```

## How does a HTTP request get passed to your Action?

There is a considerable layer of code between the HTTP protocol server that Play uses (which is [Netty](http://netty.io/) at the time of writing) and the actions you write, but this is framework code which, for most applications, you will not need to concern yourself with. In short, the Netty server and Play framework code will invoke your `Action` method (based on finding a matching mapping in the `routes` file) and pass it a Scala object of type [Request](https://playframework.com/documentation/latest/api/scala/index.html#play.api.mvc.Request).

```scala
GET      /home         HomeController.homeActionBuilderMethod

class HomeController extends Controller {
  def homeActionBuilderMethod = Action ( request -> response )
  def homeActionBuilderMethod = Action { request -> response }
}
```

## What is a Controller?

A `Controller` can be thought of as just a class that provides some context and utilities for helping you create and organise your `Action`s and their associated `Result`s.

## What is an Action?

As described by the [docs](https://www.playframework.com/documentation/2.5.x/ScalaActions), an `Action` is actually a function of type `Request => Result`. In a nutshell, an `Action` takes a `Request` object as argument (which is provided by the Play framework for us when it invokes our `Action` method  - after matching the HTTP request to a `route` defined in our routing configuration). The code block that we define in our `Action` is then invoked for us. Finally, the last expression we define in our `Action` code block, which is required to create a `Result` object, is then wrapped in a `Future` object for us (by the underlying framework) and this is then returned by the `Action` to the framework, to be computed later, asynchronously (probably by a different thread). 

Play provides us with the `ActionBuilder` trait to make the job of creating these `Action` functions easier. The `ActionFunction` trait defines the key abstract method called `invokeBlock` which any concrete `ActionBuilder` needs to override. If you study the signature of this method, it describes the general abstraction for how an `Action` behaves:- 

```scala
def invokeBlock[A](request: R[A], block: P[A] => Future[Result]): Future[Result]
```

This basically says _"Give me a `Request`, and a block of code which takes a Parameter type and generates a `Future[Result]`, and I will return you that `Future[Result]`_. Basically, the framework supplies the `Request` object, and takes care of wrapping your `Result` in a `Future` so that it can be executed somewhere later in an asynchronous fashion. All you really need to do is write the _block_ of code that generates the `Result` and the framework takes care of the rest.

In the `Action.class` (which is where `ActionBuilder` and `ActionFunction` are defined) there is a helper `Action` object which defines `invokeBlock` so that you do not have to (in general):-

```scala
object Action extends ActionBuilder[Request] {
  def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = block(request)
}
```

The type hierarchy for Actions is as follows:-

```scala
Action -> ActionBuilder -> ActionFunction
```

> In a nutshell, the framework provides our `Action` with a `Request` and also takes care of wrapping the `Result` in a `Future` to be completed asynchronously somewhere. The only thing we (generally) have to do is define the code block inside the `Action` that we would like to be executed, and which must result in the creation of a `Result`. 


## Non-blocking Actions

I believe that all the code we define in an Action, up until the final expression which generates the `Result`, will be executed in the thread that invoked the `Action`. This means that the `Future[Result]` will not be returned to the framework for completion until all the code (apart from the final `Result` creation expression) has been executed. So if we call any other services we should ensure that they are non-blocking - ie. that they do not cause a context switch on a core - otherwise this slows down the execution of Actions and potentially compromises the responsiveness of our application.




## Action Composition

...



,