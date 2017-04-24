# Play's Http Server

## Backend changes in Play v2.6

Prior to v.2.6 the default backend for Play was Netty. From v.2.6 Akka Http server became the default.  
See [Play v2.6 highlights](https://www.playframework.com/documentation/2.6.x/Highlights26) and the docs on [Akka Http](https://www.playframework.com/documentation/2.6.x/AkkaHttpServer) server.  
Note that you can still configure Play v2.6+ to use Netty as the Http server. See the [Netty docs](https://www.playframework.com/documentation/2.6.x/NettyServer) for details.

# Akka Http

The current Akka Http docs can be found [here](http://doc.akka.io/docs/akka-http/current/scala.html).

# Netty

The main class which creates and configures the underlying Netty server is [play.core.server.NettyServer](https://github.com/playframework/playframework/blob/master/framework/src/play-netty-server/src/main/scala/play/core/server/NettyServer.scala#L48)

