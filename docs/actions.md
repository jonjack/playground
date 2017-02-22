# Actions

* [What is their purpose?](#)    
* [Actions are functions](#)    
* [What are Controllers?](#)      
* [The boundary between application and outside world](#)    
* [Actions should be non-blocking](#)    
* [Which thread pool should Actions run in?](#)
* [Action composition](#)
* [Architecture](#)    
* [Articles](#)    

---

## What is their purpose?

When a request enters your Play application, the internals of the framework do some background work \(ie. create a `Request` object\), and then your application code will be called in order to execute whatever code is necessary to build the response. We need some abstraction that serves as this entry point into the application code and Actions are that abstraction.

As following suggests, Actions are the entry and exit points to your application. They are given a `Request` object by the framework, and they must return a `Result` \(the response\) - how that Result gets built is the responsibility of the developer to implement.

```scala
                              Your  Play  Application  Code
 Netty HTTP Server     +--------------------+------------------------------+
                       |                    |                              |
   Play Framework      |    Controller      |                              |
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

---



`EssentialAction` is the trait that underlies every Action. It basically takes a Request, consumes it's body \(if it has one\) and returns a Result. You can see EssentialAction and it's companion object in [`Action.scala`](https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/play/api/mvc/Action.scala#L15-L50)

## How Actions are constructed

Actions are built by Action Builders, of which there is 1 main implementation - [`ActionBuilder`](https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/play/api/mvc/Action.scala#L304) and a couple of specialized [`ActionRefiner`](https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/play/api/mvc/Action.scala#L507) versions.

All the Action builders extend [`ActionFunction`](https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/play/api/mvc/Action.scala#L254) which defines the core abstract method [`invokeBlock`](https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/play/api/mvc/Action.scala#L265) which all Action Builder implementations must provide a concrete implementation of. Here is the implementation of `invokeBlock` for the default [`ActionBuilder`](https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/play/api/mvc/Action.scala#L479), and for [`ActionRefiner`](https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/play/api/mvc/Action.scala#L517). Notice that

## How Actions are invoked

Here is an overview of how an `Action` gets called:-

1. The main entrypoint for any Play application is [`HttpRequestHandler`](https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/play/api/http/HttpRequestHandler.scala#L87)  
   When a request is received by the application, the [`DefaultHttpRequestHandler`](https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/play/api/http/HttpRequestHandler.scala#L87) calls the \(injected\) [`Router`](https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/play/api/routing/Router.scala#L15) to try and match a handler \(an Action method\) to deal with the Request.  
   Incidentally, the documentation for [`routeRequest`](https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/play/api/http/HttpRequestHandler.scala#L177-L190) explains how this method can be overridden if you need to implement some custom routing strategy.  
   So [`HttpRequestHandler`](https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/play/api/http/HttpRequestHandler.scala#L87) finds the matching Action method \(via the Router\), invokes it and returns the Action function ie. the _Handler_.

2. After various checks have been made, the server - ie. Netty \(prior to v.2.6\) or Akka Http server \(post v.2.6\) - then invokes the action function. The [`handleAction`](https://github.com/playframework/playframework/blob/master/framework/src/play-netty-server/src/main/scala/play/core/server/netty/PlayRequestHandler.scala#L256) method of [`PlayRequestHandler`](https://github.com/playframework/playframework/blob/master/framework/src/play-netty-server/src/main/scala/play/core/server/netty/PlayRequestHandler.scala#L256) is what actually executes the Action function \(inside a [for comprehension](https://github.com/playframework/playframework/blob/master/framework/src/play-netty-server/src/main/scala/play/core/server/netty/PlayRequestHandler.scala#L261-L284)\) and converts the `actionResult` into a Netty `HttpResponse`

## How requests are queued

Note that the queuing of requests is managed inside [`PlayRequestHandler`](https://github.com/playframework/playframework/blob/master/framework/src/play-netty-server/src/main/scala/play/core/server/netty/PlayRequestHandler.scala) and can be seen [here](https://github.com/playframework/playframework/blob/master/framework/src/play-netty-server/src/main/scala/play/core/server/netty/PlayRequestHandler.scala#L42) and [here](https://github.com/playframework/playframework/blob/master/framework/src/play-netty-server/src/main/scala/play/core/server/netty/PlayRequestHandler.scala#L175).

---

#### The Details

The following are examples of methods \(usually defined in a controller\) that all return an `Action` object. They all ultimately do exactly the same thing - they return the string "Hi" as a HTTP response. The `Ok` basically creates a response with a HTTP response code of `200 OK`.

```scala
  def parens =                Action ( Ok("Hi") )
  def braces =                Action { Ok("Hi") }
  def explictRequest =        Action ( request => Ok("Hi") )
  def applyParams =           Action.apply ( Ok("Hi") )
  def applyBraces =           Action.apply { Ok("Hi") }
  def applyRequest =          Action.apply ( request => Ok("Hi") )
  def makeRequestImplicit =   Action { implicit request => Ok("Hi") }
```

As described by the [docs](https://www.playframework.com/documentation/2.5.x/ScalaActions), an `Action` is actually a function of type `Request => Result`. In a nutshell, an `Action` takes a `Request` object as argument \(which is provided by the Play framework for us when it invokes our `Action` method  - after matching the HTTP request to a `route` defined in our routing configuration\). The code block that we define in our `Action` is then invoked for us. Finally, the last expression we define in our `Action` code block, which is required to create a `Result` object, is then wrapped in a `Future` object for us \(by the underlying framework\) and this is then returned by the `Action` to the framework, to be computed later, asynchronously \(probably by a different thread\).

Play provides us with the `ActionBuilder` trait to make the job of creating these `Action` functions easier. The `ActionFunction` trait defines the key abstract method called `invokeBlock` which any concrete `ActionBuilder` needs to override. If you study the signature of this method, it describes the general abstraction for how an `Action` behaves:-

```scala
def invokeBlock[A](request: R[A], block: P[A] => Future[Result]): Future[Result]
```

This basically says _"Give me a _`Request`_, and a block of code which takes a Parameter type and generates a _`Future[Result]`_, and I will return you that _`Future[Result]`. Basically, the framework supplies the `Request` object, and takes care of wrapping your `Result` in a `Future` so that it can be executed somewhere later in an asynchronous fashion. All you really need to do is write the _block_ of code that generates the `Result` and the framework takes care of the rest.

In the `Action.class` \(which is where `ActionBuilder` and `ActionFunction` are defined\) there is a helper `Action` object which defines `invokeBlock` so that you do not have to \(in general\):-

```scala
object Action extends ActionBuilder[Request] {
  def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = block(request)
}
```

The type hierarchy for Actions is as follows:-

```scala
Action -> ActionBuilder -> ActionFunction
```

> In a nutshell, the framework provides our `Action` with a `Request` and also takes care of wrapping the `Result` in a `Future` to be completed asynchronously somewhere. The only thing we \(generally\) have to do is define the code block inside the `Action` that we would like to be executed, and which must result in the creation of a `Result`.

```scala
trait Handler
         ▲
         |
trait EssentialAction
         ▲
         |
trait Action




trait ActionFunction[-R[_], +P[_]]
trait ActionBuilder[+R[_]]

trait ActionBuilder[+R[_]] extends ActionFunction[Request, R]

object Action extends ActionBuilder[Request] 

trait ActionRefiner[-R[_], +P[_]] extends ActionFunction[R, P] 

trait ActionTransformer[-R[_], +P[_]] extends ActionRefiner[R, P]
trait ActionFilter[R[_]] extends ActionRefiner[R, R]
```

```scala
// 
```

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

I think that running everything in the default execution context \(think thread pool\) ie. Actions and all other code, will mean that no matter how well you have carefully coded your actions to be non-blocking, if you do have blocking calls somewhere then your Actions will also be potentially compromised as all threads \(those executing blocking and non-blocking code alike\) will all be competing for the same set of cores anyway. So I believe the strategy of ensuring Actions only contain non-blocking operations is only effective if you have also taken steps to have blocking code run within a different execution context \(ie. a different thread pool\).

Play is designed to be asynchronous and non-blocking everywhere ....

* need to refresh on _asynchronous_ and
* need to refresh on _non-blocking_

Play APIs \(eg. WS\) are non-blocking in that they do not cause threads to block on a core \(context switched\). If you have to talk to a DB which is going to block \(eg. JDBC\) then you have to do this somewhere. I think the advice that Actions should never call any blocking code is not because it will cause the Action to delay in returning \(since all Actions are wrapped in a Future they will always be pushed somewhere else to complete asynchronously\) but I think it is assumed that to keep your app responsive you are doing any blocking work in code running in a different thread pool somewhere. As I said above, if you are doing things that may block, and using a single thread pool, then I guess you may as well block from an Action since some thread is going to block somewhere and  if you do start to bump up against the number of cores available to that single pool then having ensured your controllers are non-blocking will not save your app from potentially becoming unresponsive - having controllers that are lightning fast doesn't help if there are no spare threads to run them on \(even if they do return quickly\).

If you do blocking work in another thread pool from that servicing your controllers, then even though clients may be waiting for those blocking calls to complete \(from the requests that were passed over to the other pool to complete\), your controllers will be able to keep servicing their requests very quickly which means that new clients will still be able to make requests to your application which means it remains responsive. Even though some clients will be kept hanging  around waiting for responses, your application will appear to other clients as being responsive because controllers will still be able to take their requests.

#### Non blocking APIs

It is useful to keep in mind that if you are talking to a blocking DB \(for example\), and your application is running a lot of transactions that causes the DB to queue up requests - eg. lots of heavy reads - then it may not help to use a non-blocking API in any case ie. clients are not necessarily going to get their responses any quicker using a non-blocking API if the DB is already over-loaded - since it will be the DB itself, not the threads executing on that will be the bottleneck.

---

## Action composition

The use of the term _**composition**_ here is in the functional sense ie. if we have two functions that both take a parameter \`x\` as in \`f\(x\)\` and \`g\(x\)\`, then, if we can apply the result of one function to another, we can combine them to produce one function that applies both of them as in \`f\(g\(x\)\)\` - in this way we say that \`f\` and \`g\` are _**composable**_.

Since an \`Action\`s are functions we can _compose_ them in the same way.

---

---

### TLDR

From a logical point of view, Actions are the main gateway through which your Play application manages the requests \(from\) and responses \(to\) the clients using the application. From an implementation point of view, an `action` is an object that takes a `Request` object and returns a `Result`.

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



---

## Actions are the boundary between HTTP and your application

An `Action` is arguably one of the most fundamental features of the framework, and therefore of any Play application. They form the main boundary \(for the application developer in any case\) between any Play application code and the HTTP protocol.

As the Play application developer, you specify the mappings between the URL's that your application will serve results for, and the actions that build those results, in a `routes` file. The following example mapping will cause any HTTP request using the GET method on the `home` resource, to invoke the `homeActionBuilderMethod` \(contained in the `HomeController`\).

```scala
GET      /home         HomeController.homeActionBuilderMethod
```

## How does a HTTP request get passed to your Action?

There is a considerable layer of code between the HTTP protocol server that Play uses \(which is [Netty](http://netty.io/) at the time of writing\) and the actions you write, but this is framework code which, for most applications, you will not need to concern yourself with. In short, the Netty server and Play framework code will invoke your `Action` method \(based on finding a matching mapping in the `routes` file\) and pass it a Scala object of type [Request](https://playframework.com/documentation/latest/api/scala/index.html#play.api.mvc.Request).

```scala
# 
GET      /home         HomeController.homeActionBuilderMethod

class HomeController extends Controller {
  def homeActionBuilderMethod = Action ( request -> response )
  def homeActionBuilderMethod = Action { request -> response }
}
```

## 

## What is an Action?

## Helper companion objects for creating actions

```scala
object EssentialAction {

    def apply(f: RequestHeader => Accumulator[ByteString, Result]): EssentialAction = new EssentialAction {
    def apply(rh: RequestHeader) = f(rh)
  }
}

object Action extends ActionBuilder[Request] {

  def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = block(request)
}
```

## Non-blocking Actions

I believe that all the code we define in an Action, up until the final expression which generates the `Result`, will be executed in the thread that invoked the `Action`. This means that the `Future[Result]` will not be returned to the framework for completion until all the code \(apart from the final `Result` creation expression\) has been executed. So if we call any other services we should ensure that they are non-blocking - ie. that they do not cause a context switch on a core - otherwise this slows down the execution of Actions and potentially compromises the responsiveness of our application.

## Action Composition

...

,

---

---

## \#\# ARTICLE STARTS BELOW \(ABOVE IS ALL TEMPORARY CONTENT\)

## Some features of Actions

1. **They are at the boundary of any Play application** and are generally the first piece of your \(non-framework\) code to be executed when a request comes in. Actually, you can write filters as well which will get executed before your action code, but these are not a mandatory part of your application whereas you have to write Actions since they are the request handlers of your application.
2. **Actions are functions** which basically map a Request to a Result \(\```Request => Result`)``

## Action Architecture

---

## Designing your own Actions

In a [conversation](https://groups.google.com/d/msg/play-framework/zpql5zjDoAM/tyBUkIH2AwAJ) with Will Sargent \(Lightbend Engineer\) on the Google Play Group forum, he states:-

> "_You should always create your own actions, controllers and request types on top of Play.  This will give you flexibility to add your own context and domain specific information in your classes._"

Here is an example of a custom [PostAction](https://github.com/playframework/play-rest-api/blob/master/app/v1/post/PostAction.scala) from Will's REST API sample that is designed to handle a POST request. It extends `ActionBuilder` to take a `PostRequest` rather than a plain old `Request` and

## Articles

[All Actions are asynchronous by default](https://groups.google.com/d/msg/play-framework-dev/30MqnKDp0Fs/25PU-Y0RhGoJ) - very good blog post by James Roper on how Actions work behind the scenes. Very insightful, especially if you are unsure of the difference between `Action.apply` and `Action.async`. He also talks \(in the same thread\) about [blocking actions and execution contexts](https://groups.google.com/d/msg/play-framework-dev/30MqnKDp0Fs/Hz5mKs4NVpIJ) and how wrapping some blocking I/O code in a `Future` does not magically make the I/O asynchronous and therefore the action will be synchronous. The performance implications require an understanding of how you can have blocking code run in a different thread pool.

