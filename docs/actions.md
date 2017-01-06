# Controllers & Actions

An `Action` is arguably one of the most fundamental features of the framework, and therefore of any Play application. They form the main boundary - for the developer in any case - between the web application code and the HTTP protocol. There is a considerable layer of code between the HTTP protocol server (which is [Netty](http://netty.io/) at the time of writing) and the actions you write, but this is framework code which, for most applications, you will not need to concern yourself with.

A `Controller` can be thought of as just a class that provides some context and utilities for helping you create and organise your `Action`s and their associated `Result`s.

## What is an Action?

As described by the [docs](https://www.playframework.com/documentation/2.5.x/ScalaActions), an `Action` is actually a function of type `Request => Result`. Play provides us with the `ActionBuilder` trait to make the job of creating these `Action` functions easier.

The `ActionFunction` trait defines the key abstract method called `invokeBlock` which any concrete `ActionBuilder` needs to override. If you study the signature of this method, it describes the general abstraction for how an `Action` behaves:- 

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

## Action Composition





