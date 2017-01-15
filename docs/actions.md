# Controllers & Actions

## Actions are the boundary between HTTP and your application 

An `Action` is arguably one of the most fundamental features of the framework, and therefore of any Play application. They form the main boundary (for the application developer in any case) between any Play application code and the HTTP protocol. 

An `action` is an object that takes a `Request` object and returns a `Result`. 

```scala 
       |                           Host Machine                         |
       |                 CPU Cores / Memory / Disk / Files              |
       |                               JVM                              |
       |                           Netty Server                         |
       |                                                                |
       |                                                                |
Http --|--> Framework --> Request --> Action --> Result --> Framework --|--> Http
       |    URI route                   |                               |
       |     mapping                    v                               |
       |                      Rest of Application code                  |
                                    Web Services
                                     Datastores
                                       Cache              
```

As the Play application developer, you specify the mappings between the URL's that your application will serve results for, and the actions that build those results, in a `routes` file. The following example mapping will cause any HTTP request using the GET method on the `home` resource, to invoke the `homeActionBuilderMethod` (contained in the `HomeController`).

```scala
GET      /home         HomeController.homeActionBuilderMethod
```

## How does a HTTP request get passed to your Action?

There is a considerable layer of code between the HTTP protocol server that Play uses (which is [Netty](http://netty.io/) at the time of writing) and the actions you write, but this is framework code which, for most applications, you will not need to concern yourself with. 

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

#### A note on execution contexts

I believe that running everything in the default execution context ie. Actions and all other code, will mean that no matter how well you have carefully coded your actions to be non-blocking, if you do have blocking calls somewhere then your Actions will also be potentially compromised as all threads (those executing blocking and non-blocking code alike) will all be competing for the same number of cores anyway. So I believe the strategy of ensuring Actions only contain non-blocking operations is only effective if you have also taken steps to have blocking code run within a different execution context (ie. a different thread pool). 

## Action Composition

...



