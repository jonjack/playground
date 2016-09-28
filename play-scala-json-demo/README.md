## Simple JSON Demo using Play and Scala

Very basic tutorial for getting a small Scala application up and running using the Play Framework.

### Requirements:

Download [Typesafe](https://www.typesafe.com/get-started)'s [Activator](http://downloads.typesafe.com/typesafe-activator/1.3.5/typesafe-activator-1.3.5.zip?_ga=1.202425235.308194262.1434196995) and add it to your PATH.

You can either download the packge containing a pre-populated Ivy repository or just get the mini package (this will take a few minutes to download all the necessary dependencies on the initial run). If you are behind a proxy check the [docs](https://www.typesafe.com/activator/docs) for the config.

The [Scala IDE](http://scala-ide.org/) is the recommended tool of choice for working with Play/Scala projects.

---

### 1. Create an Application Using Activator

> Activator is a wrapper around the Simble Build Tool (SBT). It adds some extra SBT commands for working with Typesafe projects.

With Activator on the path:

```scala
$ activator new
```

Choose a Template seed:
```scala
Choose from these featured templates or enter a template name:
  1) minimal-akka-java-seed
  2) minimal-akka-scala-seed
  3) minimal-java
  4) minimal-scala
  5) play-java
  6) play-scala

Hit tab to see all 270 odd templates.

> 6

Enter a name for your application (just press enter for play-java)
> play-scala-demo

OK, application "play-scala-demo" is being created using the "play-scala" template.

```

### 2. Initialize the Project

```scala

$ cd play-scala-demo
$ activator                 // this will build the project and resolve all dependencies
```

### 3. Import it into your IDE, eg. Eclipse

You need to use the SBT plugin [sbteclipse](https://github.com/typesafehub/sbteclipse/wiki/Using-sbteclipse) to generate the files necessary for IDE support.


> Previous versions of Play packaged the sbteclipse plugin but this has been removed as of v2.4.0 - probably to make the projects more generic and streamlined.

Edit `project\plugins.sbt` to include the sbteclipse plugin:

```scala
addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "4.0.0-RC2")
```

Run `activator` to download and install plugin:

```scala
$ activator
```

Now you should be able to run the following to generate the Eclipse project files:

```scala
$ activator eclipse
```

### 4. Check it Runs

```bash
activator run 
```

The project seed should be running at [http://localhost:9000/](http://localhost:9000/)


### 5. Check out the Activator UI

Stop the application if it still running (Ctrl + C), then run the `ui` like as:

```bash
activator ui 
```

The Activator UI should start automatically in your default browser, but if it doesn't try [http://127.0.0.1:8888/app/play-scala-demo/#run/system](http://127.0.0.1:8888/app/play-scala-demo/#run/system).

The Activator UI is a sort of development play ground for creating / inspecting Typesafe stack projects. Have a play!


### 6. Lets add an Object

If you haven't done it already, import the project into Eclipse and then create a `models` folder under `app` and add the following case class:

```scala
package models

import play.api.libs.json._

case class Customer ( 
    
    val id:                       String,
    val title:                    String,
    val firstName:                String,
    val surname:                  String,
    val dateOfBirth:              String,
    val primaryTelephoneNumber:   String,
    val primaryTelephoneType:     String
    
)
{
	implicit val customerWrites = Json.writes[Customer]
	val asJson: JsValue = Json.toJson(this)
}
```

Add a companion object to give us a test object. This should live in the same source file as the above case class.

```scala
object Customer {
  
    def getTestCustomer = {  
      val id = "0010012404509"
      val title = "Ms"
      val firstName = "Tera"
      val surname = "Patrick"
      val dateOfBirth = "10/10/1976"
      val primaryTelephoneNumber = "07905576653"
      val primaryTelephoneType = "mobile"
      Customer(id, title, firstName, surname, dateOfBirth, primaryTelephoneNumber, primaryTelephoneType)
    }
    
}
```

### 7. Now Lets Expose it as a JSON Message

Create a CustomerAPI controller with an Action:

```scala
package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import models.Customer

class CustomerAPI extends Controller {
  
    def customersv1 = Action {
      val customer = Customer.getTestCustomer
      var jsonData: JsValue = customer.asJson
      val json: JsValue = Json.obj("status" -> "SUCCESS", "version" -> "v2.0", "data" -> jsonData, "errors" -> "","meta" -> "")
      Ok(json)
  }

}
```

> Note the Scala IDE highlights a `var jsonData` in red to warn us that we are using mutable state


### 8. Now lets configure the Route

In the `routes` file configure the mapping between the request and the Action that will handle it:

```bash
GET   /api/v1/customers          	controllers.CustomerAPI.customersv1
```

- Note how the IDE makes suggestions for the URL because of the available regex properties and also the autocompletion on the Controller - this is because the `routes` file - as with alot of Play - is compiled so we catch problems even with our configuration files at compile time.
- add a typo now to `customersv1` -> `customersv2` to demonstrate the compile time safety in the `compile` step next


### 9. Build the App

Using the SBT console:

```bash
activator compile
```

We have a failure because the routing is invalid - Play is able to tell us this because the `routes` file is compiled.

See:

```bash
/target/scala-2.11/routes/main/router/Routes.scala
```


### 10. Fix and Recompile

Fix the `routes` file `customersv2` -> `customersv1`

```bash
activator compile
```

We should now see success.


### 11. Run the Project in Development Mode

Start the application with the incremental compiler enabled.

```bash
activator run
```

Check out the results at [http://localhost:9000/api/v1/customers](http://localhost:9000/api/v1/customers)

```bash
{
	status: "SUCCESS",
	version: "v2.0",
	data: {
		id: "0010012404509",
		title: "Ms",
		firstName: "Tera",
		surname: "Patrick",
		dateOfBirth: "10/10/1976",
		primaryTelephoneNumber: "07905576653",
		primaryTelephoneType: "mobile"
	},
	errors: "",
	meta: ""
}
```


### 12. Test the Incremental Compiler

The `"version" -> "v2.0"` in the `CustomerAPI` controller is incorrect - it should read v1.0, so lets change it.

```bash
val json: JsValue = Json.obj(... "version" -> "v1.0"...)
```

Refresh our result page and we should see the recompilation occur in the terminal and the change reflected in our JSON message.

```bash
{
	status: "SUCCESS",
	version: "v1.0",
	data: {
		id: "0010012404509",
		title: "Ms",
		firstName: "Tera",
		surname: "Patrick",
		dateOfBirth: "10/10/1976",
		primaryTelephoneNumber: "07905576653",
		primaryTelephoneType: "mobile"
	},
	errors: "",
	meta: ""
}
```


### 13. Turn on the Watching

If we start the application in development mode this way then the compiler goes 1 step further for us:

```bash
activator ~run
```

Now we want to make some more code changes - check the terminal as we make the changes:

- We don't need jsonData to be a `var` we can change it to a `val`

Now we realise we need a `brand` property on `Customer`:

```bash
val brand:                    String
```

Compilation fails because we are using a `case` class.
- Note how the `case class Customer` is taking paramaters at the class level. These are automatically fed to the implicit constructor for the class.

Add the `brand` to the test constructor:

```bash
val brand ="BG"
Customer(... primaryTelephoneType, brand)
```

> Notice all the while how recomplation is happening in the background for us with no effort on our part. We wouldn't use this in a production or performance testing ennvironment for sure but for local development its arguably a very useful feature.

Refresh our results page. We can see the brand added but no recompilation had to occur.

```bash
{
	status: "SUCCESS",
	version: "v1.0",
	data: {
		id: "0010012404509",
		title: "Ms",
		firstName: "Tera",
		surname: "Patrick",
		dateOfBirth: "10/10/1976",
		primaryTelephoneNumber: "07905576653",
		primaryTelephoneType: "mobile",
		brand: "BG"
	},
	errors: "",
	meta: ""
}
```

### 14. Now the business want v2.0

The problem at the moment is that the JSON generation is being done for us by Play's own Json API on top of `Jackson`.

An option is to pick what we want by defining our own customer Writes[T]. Add the following to the `CustomerAPI` controller:

```scala

    implicit val customersV2 = new Writes[Customer] {
      def writes(customer: Customer) = Json.obj(
      "id" -> customer.id,
      "title" -> customer.firstName,
      "dob" -> customer.dateOfBirth)
    }
        
    def customersv2 = Action {
      val customer = Customer.getTestCustomer
      val jsonData = Json.toJson(customer)
      val json: JsValue = Json.obj("status" -> "SUCCESS", "version" -> "v2.0", "data" -> jsonData, "errors" -> "","meta" -> "")
      Ok(json)
    }
```

Add a new route in conf/routes:

```bash
GET   /api/v2/customers          	controllers.CustomerAPI.customersv2
```

There should be no need to rebuild/redeploy etc since the incremental compiler should have taken care of everything for us.     

Check the new resource works at [http://localhost:9000/api/v2/customers](http://localhost:9000/api/v2/customers).

---    

### Summary

TODO









