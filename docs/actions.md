# Actions

* [Actions in a nutshell - TLDR](#)    
* [Actions are functions](#)    
* [What are Controllers?](#)      
* [The boundary between application and outside world](#)    
* [Actions should be non-blocking](#)    
* [Which thread pool should Actions run in?](#)
* [Action composition](#)
* [Architecture](#)    
* [Articles](#)    


---

## Actions in a nutshell - TLDR

This article documents a lot of nitty gritty details about Actions. This section is a quick summary of the fundamental features:-

- #### They are at the boundary of any Play application
  Actions are generally the first piece of your \(non-framework\) code to be executed when a request comes in. Actually, you can write filters as well which will get executed before your action code, but these are not a mandatory part of your application whereas you have to write Actions since they are the request handlers of any application. Actions are therefore the entry and exit points to your application. They are given a `Request` object by the framework, and they must return a `Result` \(the response\) - how that Result gets built is the responsibility of the developer to implement.

- #### Action methods return Actions, not Results

  When you declare an entry (a request mapping) in the `routes` file, you map a request pattern to some Action method. Note that this method does not execute the Action and return a Result, it just triggers the construction of the Action object and returns that.  
  
  ```scala
  def actionMethod  =  Action { request => [some result] } 
   
  // with explicit return type
  def actionMethod: Action[AnyContent]  =  Action { request => [some result] } 
   ```

- #### The framework executes the Action
  As stated above, the Action method will return the constructed Action instance. The framework will execute the Action later. See [How Actions are invoked](#) section for more details.

- #### `Request => Future[Result]`
  Actions can be thought of as just simple functions that take a `Request` and return a `Result`, or rather, they are an object that encapsulate a function of type `Request => Result`. When you construct them, you need to supply the `Request => Result` function as an argument (see next)

- #### Constructors - `apply` and `async`
 All Action's, generally, have two constructors - `apply` and `async`
  
  `apply` returns a `Result`
  `async` returns a `Future[Result]`
  
 ```scala
 def actionMethod  =  Action       {request => [some result] }  // gets expanded to a call to .apply() method
 def actionMethod  =  Action.apply {request => [some result] }  // same as sugared version above
 def actionMethod  =  Action.async {request => Future[some result] }
   ```

  See [How Actions are constructed](#) section below for more details.

- #### All Actions are asynchronous

  It is a common misconception that there are two types of action:- **synchronous** and **asynchronous**.
  But this is not the case, since all Actions are computed asynchronously by the framework, regardless of which factory method you call (`apply` or `async`):-
  
 `Action.async(request => Future[Result])`
 `Action.apply(request => Result)`

 Invoking `apply` (explicitly or not) ends up in a call to one of the `async` functions anyway (behind the scenes), so every Action ends up returning a `scala.concurrent.Future` which will be executed asynchronously by the Play framework.

- #### The Action "_body_" is actually an anonymous function argument
Since Actions generally (unless you are implementing your own), just take a single function argument of type `Request => Result`. The use of braces (allowed in Scala for single argument only functions) seems to be the idiom, but this may be a bit confusing to the newcomer to Scala since, at first glance, this can look like the body of the Action. The following examples, are analagous to the above ones and use parens to show that the function is being passed as an argument to either the `apply` or `async` factory methods.

 ```scala
 def actionMethod  =  Action       (request => [some result]) // expanded to call to .apply() method
 def actionMethod  =  Action.apply (request => [some result])  // same as sugared version above
 def actionMethod  =  Action.async (request => Future[some result])
   ```
  **\*\* So it may look, at first glance, that when you are coding your Action methods, you are implementing the body of the Action. But this is not so. What you are actually doing is implementing the body of the anonymous function that you are passing to the Action constructor method (`apply` or `async`) as an argument.**


## Actions are a function of type `Request => Future[Result]`

If you look at the source for the [Action trait](https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/play/api/mvc/Action.scala#L65-L117) you will see that an `Action` is just some class with an `apply` method that takes a `Request` and returns a `Future[Result]`. 

Here is [an example](https://github.com/jonjack/play-rest-api/blob/master/app/v1/post/PostAction.scala#L27-L54) of a custom Action with it's own custom implementation of `invokeBlock`. And here is the [controller](https://github.com/jonjack/play-rest-api/blob/master/app/v1/post/PostController.scala#L16-L44) that returns instances of that Action. 

## How Actions are constructed

Actions are built by Action Builders, of which there is 1 main implementation - [`ActionBuilder`](https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/play/api/mvc/Action.scala#L304) and a couple of specialized [`ActionRefiner`](https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/play/api/mvc/Action.scala#L507) versions.

All the Action builders extend [`ActionFunction`](https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/play/api/mvc/Action.scala#L254) which defines the core abstract method [`invokeBlock`](https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/play/api/mvc/Action.scala#L265) which all Action Builder implementations must provide a concrete implementation of. Here is the implementation of `invokeBlock` for the default [`ActionBuilder`](https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/play/api/mvc/Action.scala#L479), and for [`ActionRefiner`](https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/play/api/mvc/Action.scala#L517).

Consider this action method.

```scala
def index = Action { implicit request =>
    Ok("Hi")
  }
```

- The invocation of `index` will construct and return an instance of `Action`
- Invoking `index` does not return the result to the client, it returns the Action which will be executed later by the framework
- The above is equivalent to the following call which explicitly calls the `apply` method, passing a block of code which creates a `Result`. 

 ```scala
def index = Action.apply ( Ok("Hi") )
```

 If you trace what type `Ok` is, you will see that it creates an object of type [`Status`](https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/play/api/mvc/Results.scala#L388-L400), the `apply` method of which you can see returns a `Result`. 

Note that the call to `Action` (expanded by the compiler to `Action.apply`) uses a convenience [Action object](https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/play/api/mvc/Action.scala#L495-L499). This object does not have an `apply` method however, but it does extend the [trait ActionBuilder](https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/play/api/mvc/Action.scala#L304-L452) which does.

> #### Changes in creating Actions in Play v2.6
Prior to v2.6, you would generally use the convenience object `Action` (which extended `ActionBuilder`) to construct an `Action`. This is now [deprecated](https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/play/api/mvc/Action.scala#L494) however, and since v2.6 you are now encouraged to inject an `ActionBuilder` like [`DefaultActionBuilder`](https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/play/api/mvc/Action.scala#L473).

#### Note that all the following examples of creating an Action are the same

```scala
def parens =                Action ( Ok("Hi") )
def braces =                Action { Ok("Hi") }
def explictRequest =        Action ( request => Ok("Hi") )
def applyParams =           Action.apply ( Ok("Hi") )
def applyBraces =           Action.apply { Ok("Hi") }
def applyRequest =          Action.apply ( request => Ok("Hi") )
def makeRequestImplicit =   Action { implicit request => Ok("Hi") }
```


## How Actions are invoked

Here is an overview of how an `Action` gets called:-

1. The main entrypoint for any Play application is [`HttpRequestHandler`](https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/play/api/http/HttpRequestHandler.scala#L87)  
   When a request is received by the application, the [`DefaultHttpRequestHandler`](https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/play/api/http/HttpRequestHandler.scala#L87) calls the \(injected\) [`Router`](https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/play/api/routing/Router.scala#L15) to try and match a handler \(an Action method\) to deal with the Request.  
   Incidentally, the documentation for [`routeRequest`](https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/play/api/http/HttpRequestHandler.scala#L177-L190) explains how this method can be overridden if you need to implement some custom routing strategy.  
   So [`HttpRequestHandler`](https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/play/api/http/HttpRequestHandler.scala#L87) finds the matching Action method \(via the Router\), invokes it and returns the Action function ie. the _Handler_.

2. After various checks have been made, the server - ie. Netty \(prior to v.2.6\) or Akka Http server \(post v.2.6\) - then invokes the action function. The [`handleAction`](https://github.com/playframework/playframework/blob/master/framework/src/play-netty-server/src/main/scala/play/core/server/netty/PlayRequestHandler.scala#L256) method of [`PlayRequestHandler`](https://github.com/playframework/playframework/blob/master/framework/src/play-netty-server/src/main/scala/play/core/server/netty/PlayRequestHandler.scala#L256) is what actually executes the Action function \(inside a [for comprehension](https://github.com/playframework/playframework/blob/master/framework/src/play-netty-server/src/main/scala/play/core/server/netty/PlayRequestHandler.scala#L261-L284)\) and converts the `actionResult` into a Netty `HttpResponse`

## How requests are queued

Note that the queuing of requests is managed inside [`PlayRequestHandler`](https://github.com/playframework/playframework/blob/master/framework/src/play-netty-server/src/main/scala/play/core/server/netty/PlayRequestHandler.scala) and can be seen [here](https://github.com/playframework/playframework/blob/master/framework/src/play-netty-server/src/main/scala/play/core/server/netty/PlayRequestHandler.scala#L42) and [here](https://github.com/playframework/playframework/blob/master/framework/src/play-netty-server/src/main/scala/play/core/server/netty/PlayRequestHandler.scala#L175).


## Designing your own Actions

On a Google Play Group forum [thread](https://groups.google.com/d/msg/play-framework/zpql5zjDoAM/tyBUkIH2AwAJ), Will Sargent \(Lightbend Engineer\) states:-

> "_You should always create your own actions, controllers and request types on top of Play.  This will give you flexibility to add your own context and domain specific information in your classes._"

Here is an example of a custom [PostAction](https://github.com/playframework/play-rest-api/blob/master/app/v1/post/PostAction.scala) from Will's REST API sample that is designed to handle a POST request. It extends `ActionBuilder` to take a `PostRequest` rather than a plain old `Request`.


## Action Composition

The use of the term _**composition**_ here is in the functional sense ie. if we have two functions that both take a parameter \`x\` as in \`f\(x\)\` and \`g\(x\)\`, then, if we can apply the result of one function to another, we can combine them to produce one function that applies both of them as in \`f\(g\(x\)\)\` - in this way we say that \`f\` and \`g\` are _**composable**_.

Since an \`Action\`s are functions we can _compose_ them in the same way.

## Walkthrough of executing an Action

I invoked the following action (Play v2.5) and recorded some of the route below. 

```scala
def index = Action { Ok(views.html.index("")) }
```

#### Some of the places the thread landed in

```scala
// Here is where we start - with a call to the Action companion object
Action { Ok(views.html.index("")) }

// The Action companion object looks like this - note there is no apply method
object Action extends ActionBuilder[Request] {
  def 'invokeBlock[A]'(request: Request[A], block: (Request[A]) => Future[Result]) = block(request)
}

// Since the above does not have an `apply` method and the call to Action { ... } will be expanded 
// to a call to Action.apply { ... } we therefore move up to the nearest implementation of `apply`  
// which is in the parent trait ActionBuilder
// This is 
trait ActionBuilder {
final def apply[A](bodyParser: BodyParser[A])(block: R[A] => Result): Action[A] = async(bodyParser) { req: R[A] =>
    'Future.successful(block(req))'
}

// The above `apply` method delegates to the `async` method to build the Action
trait ActionBuilder {
final def async[A](bodyParser: BodyParser[A])(block: R[A] => Future[Result]): Action[A] = composeAction(new Action[A] {
    def parser = composeParser(bodyParser)
    def 'apply'(request: Request[A]) = try {
      invokeBlock(request, block)
    } catch {
      // NotImplementedError is not caught by NonFatal, wrap it
      case e: NotImplementedError => throw new RuntimeException(e)
      // LinkageError is similarly harmless in Play Framework, since automatic reloading could easily trigger it
      case e: LinkageError => throw new RuntimeException(e)
    }
    override def executionContext = ActionBuilder.this.executionContext
  })

// When the Action gets invoked, it's the above `apply` method will be invoked, which calls `invokeBlock`
// Now we are back in the companion object which defined `invokeBlock`
object Action extends ActionBuilder[Request] {
  def 'invokeBlock[A]'(request: Request[A], block: (Request[A]) => Future[Result]) = block(request)
}
```

> I never finished the above off since I got so pissed off trying to trace the request through all the classes and methods of the `Action` class so I gave up. How Actions are constructed and invoked seems very convoluted, but it' probably my lack of understanding that makes it seem this way.

#### What I took from this pointless exercise was that I should just get a high level understanding of Actions and leave it there. Maybe it will become clearer later when my Scala gets better! ;)

## Which thread pool should Actions run in?

I think that running everything in the default execution context \(think thread pool\) ie. Actions and all other code, will mean that no matter how well you have carefully coded your actions to be non-blocking, if you do have blocking calls somewhere then your Actions will also be potentially compromised as all threads \(those executing blocking and non-blocking code alike\) will all be competing for the same set of cores anyway. So I believe the strategy of ensuring Actions only contain non-blocking operations is only effective if you have also taken steps to have blocking code run within a different execution context \(ie. a different thread pool\).

Play is designed to be asynchronous and non-blocking everywhere ....

* need to refresh on _asynchronous_ and
* need to refresh on _non-blocking_

Play APIs \(eg. WS\) are non-blocking in that they do not cause threads to block on a core \(context switched\). If you have to talk to a DB which is going to block \(eg. JDBC\) then you have to do this somewhere. I think the advice that Actions should never call any blocking code is not because it will cause the Action to delay in returning \(since all Actions are wrapped in a Future they will always be pushed somewhere else to complete asynchronously\) but I think it is assumed that to keep your app responsive you are doing any blocking work in code running in a different thread pool somewhere. As I said above, if you are doing things that may block, and using a single thread pool, then I guess you may as well block from an Action since some thread is going to block somewhere and if you do start to bump up against the number of cores available to that single pool then having ensured your controllers are non-blocking will not save your app from potentially becoming unresponsive - having controllers that are lightning fast doesn't help if there are no spare threads to run them on \(even if they do return quickly\).

If you do blocking work in another thread pool from that servicing your controllers, then even though clients may be waiting for those blocking calls to complete \(from the requests that were passed over to the other pool to complete\), your controllers will be able to keep servicing their requests very quickly which means that new clients will still be able to make requests to your application which means it remains responsive. Even though some clients will be kept hanging around waiting for responses, your application will appear to other clients as being responsive because controllers will still be able to take their requests.

#### Non blocking APIs

It is useful to keep in mind that if you are talking to a blocking DB \(for example\), and your application is running a lot of transactions that causes the DB to queue up requests - eg. lots of heavy reads - then it may not help to use a non-blocking API in any case ie. clients are not necessarily going to get their responses any quicker using a non-blocking API if the DB is already over-loaded - since it will be the DB itself, not the threads executing on that will be the bottleneck.

## Articles

[All Actions are asynchronous by default](https://groups.google.com/d/msg/play-framework-dev/30MqnKDp0Fs/25PU-Y0RhGoJ) - very good blog post by James Roper on how Actions work behind the scenes. Very insightful, especially if you are unsure of the difference between `Action.apply` and `Action.async`. He also talks \(in the same thread\) about [blocking actions and execution contexts](https://groups.google.com/d/msg/play-framework-dev/30MqnKDp0Fs/Hz5mKs4NVpIJ) and how wrapping some blocking I/O code in a `Future` does not magically make the I/O asynchronous and therefore the action will be synchronous. The performance implications require an understanding of how you can have blocking code run in a different thread pool.





