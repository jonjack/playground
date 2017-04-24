## What is a Controller?

A `Controller` can be thought of as just a class that provides some context and utilities for helping you create and organise your `Actions` and their associated `Results`

Each request coming into our application needs to invoke some method which returns an `Action` object appropriate for dealing with that request. We therefore need somewhere to define these methods. This is what Controllers are for.

An example **Controller** with methods that return an Action to handle the request for `/home`

```scala
class HomeController extends Controller {

  def homeActionBuilderMethod = Action ( request -> response )

  // or more commonly (since there is 1 function argument) braces are used 
  def homeActionBuilderMethod = Action { request -> response }
}
```

> #### Controller design pattern
>
> I believe the use of the term _Controller_ \(in Play\) stems from the _Front Controller_ pattern which is a [structural design pattern](https://en.wikipedia.org/wiki/Software_design_pattern#Structural_patterns) intended to act a single, central entry point to an application. Controllers in Play can generally be thought of more in terms of _Page Controllers_ however \(although they are intended to deal with all response types not just _pages_\), since they are generally implemented at a more granular level than a Front Controller ie. you implement controllers for different parts of your application rather than having just a single controller.



