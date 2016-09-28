
## Random collection of Play framework research projects

### Requirements for running Play.

 [![Build Status](https://travis-ci.org/jonjack/jagerbomb.svg?branch=master)](https://travis-ci.org/jonjack/jagerbomb)


### Requirements:

Download [Typesafe](https://www.typesafe.com/get-started)'s [Activator](http://downloads.typesafe.com/typesafe-activator/1.3.5/typesafe-activator-1.3.5.zip?_ga=1.202425235.308194262.1434196995) and add it to your PATH.

You can either download the packge containing a pre-populated Ivy repository or just get the mini package (this will take a few minutes to download all the necessary dependencies on the initial run). If you are behind a proxy check the [docs](https://www.typesafe.com/activator/docs) for the config.

The [Scala IDE](http://scala-ide.org/) is the recommended tool of choice for working with Play/Scala projects.

---

#### Architecture

The application uses the [Play framework](https://www.playframework.com/).      
The following shows the basic layout (more advanced explanantion [here](https://www.playframework.com/documentation/2.4.2/Anatomy)):-

```bash
jagerbomb
	|
	|
   	|- app 						// dynamic code				
    |  	|									
    |  	|- controllers 			// Scala or Java controller classes
    |  	|- models				// Scala or Java model classes
    |  	|- views				// Scala view templates
    |
    |
   	|- public 					// static code				
      	|									
      	|- images 				// 
      	|- javascripts			// 
      	|- stylesheets			// 

```

#### Resources

The following are some useful resources on the official docs site:-

[Index](https://www.playframework.com/documentation/2.4.2/ScalaHome)     
[UI templates](https://www.playframework.com/documentation/2.4.2/ScalaTemplates)     
[UI assets](https://www.playframework.com/documentation/2.4.2/Assets)     


#### Running the application

When you have added Activator to your path, run this in a shell prompt. Note you will need an internet connection because it will download a load of required dependencies first time.

```bash
$ activator run
```

This should start the project at [http://localhost:9000/](http://localhost:9000/).


---

#### Tools

___Eclipse___    
Run the following to generate the Eclipse project files:

```bash
$ activator eclipse
```

You should then be able to import it as an existing project into Eclipse.

___Sublime___    
You could also just open it in Sublime or something if you are not writing Java code.


