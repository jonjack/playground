
> Still on the TODO list!

---

# play-api-template

R&D for serving an api off of Play (Scala).

The template will aim to provide an example of:-

- using Play instance(s) for managing downstream request processing, authentication and authorization management, caching
- using Akka cluster for providing a scalable business tier
- incorporation of OpenID for authentication
- some basic OAuth type flow for authorization of resource access


---

## Test run inside Docker

#### With Docker installed, get my Jalpine image

```bash
docker pull jonjack/jalpine
```

#### With Activator installed, create a Play-Scala image from template

```bash
activator new
choose 6 for Play-Scala
'play-api-test'
```

#### Create a distribution from the Play template

```bash
cd play-api-test
activator dist
```

#### Now start a container

```bash
docker run -it -p 80:80 -v /Users/jonjack/Documents/Kitematic/jalpine:/app jonjack/jalpine /bin/sh
```

Note that when using Kitematic, the volume path needs a path on the host somewhere inside the Kitematic directory (otherwise the container stops for some reason when you try and access the mount point - I think this is a bug).

Copy the 'play-api-test-SNAPSHOT.zip' created above into the mount point on the host ie. `/Users/jonjack/Documents/Kitematic/jalpine`.

The file should be visible in the container under `/app`.    

Now unzip the distribution.

```bash
unzip play-api-test-SNAPSHOT.zip
```

Check the start script and if the execute permissions are not set, then set them.

```bash
chmod +x /app/play-api-test-1.0-SNAPSHOT/bin/play-api-test
```

The Play distribution startup script has a dependency on bash so we need to install it (since our Alpine image only has the bourne `sh` shell by default).


```bash
# add latest repo for good measure
echo 'http://dl-4.alpinelinux.org/alpine/v3.3/main' >> /etc/apk/repositories

# update the local package cache
apk upgrade --update

# add bash
apk add bash
```

We started the container with `-p 80:80` but Play runs on port `9000` by default so we need to chnage this to `80` when we invoke the start script.

```bash
bin/play-api-test -Dhttp.port=80
```

If it ran then you shoud be able to see the basic page served by Play at [http://192.168.99.100/](http://192.168.99.100/).

It should simply display the default message "Your new application is ready."

