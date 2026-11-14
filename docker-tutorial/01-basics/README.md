# Module 01 - Docker Basics (image, container, lifecycle)

**Concepts:** image vs container, Dockerfile instructions, port mapping, env vars,
logs/exec/inspect, restart policies, copy files, tag/save/load, cleanup.

## 1. Build the jar and the image
Run from the **repository root** (`docker-tutorial/`):

```bash
mvn -f apps/hello-app/pom.xml clean package -DskipTests      # creates apps/hello-app/target/app.jar
docker build -f 01-basics/Dockerfile -t hello-app:basic --build-arg APP_VERSION=1.0.1 apps/hello-app
docker images hello-app
docker history hello-app:basic            # see layers
docker inspect --format '{{json .Config.Labels}}' hello-app:basic
```

## 2. Run containers
```bash
docker run -d --name hello -p 8080:8080 hello-app:basic     # -d detached, -p host:container
curl http://localhost:8080/hello
curl http://localhost:8080/actuator/health
```

Override env vars at runtime:
```bash
docker run -d --name hello2 -p 8081:8080 -e APP_GREETING="Hi from env" hello-app:basic
curl http://localhost:8081/hello
curl http://localhost:8081/env
```
Random host port: `docker run -d -P hello-app:basic` then `docker port <container>`.

## 3. Inspect & debug a running container
```bash
docker ps                                  # running  (-a for all)
docker logs -f hello                       # follow logs
docker exec -it hello sh                   # shell inside  (try: whoami, ps, env, ls /app)
docker top hello                           # processes
docker stats --no-stream                   # CPU/mem
docker inspect hello | less                # full JSON (IP, mounts, env ...)
docker cp hello:/app/app.jar ./copied.jar  # copy file out (and in: docker cp file hello:/tmp/)
docker diff hello                          # files changed in container layer
```

## 4. Lifecycle & signals
```bash
docker stop hello        # SIGTERM, then SIGKILL after 10s (graceful shutdown in Spring Boot!)
docker start hello
docker restart hello
docker kill hello        # SIGKILL immediately
docker rm -f hello hello2
```
Graceful shutdown test: `curl "localhost:8080/slow?seconds=8" &` then `docker stop hello` - the
request still completes because `server.shutdown=graceful`.

## 5. Restart policies
```bash
docker run -d --name crashy --restart unless-stopped -p 8082:8080 hello-app:basic
docker kill crashy; sleep 3; docker ps       # it comes back automatically
# policies: no | on-failure[:N] | always | unless-stopped
docker rm -f crashy
```

## 6. ENTRYPOINT vs CMD
```bash
docker build -f 01-basics/Dockerfile.entrypoint-vs-cmd -t echo-demo 01-basics
docker run --rm echo-demo                # Hello World
docker run --rm echo-demo Architect      # Hello Architect   (CMD overridden)
docker run --rm --entrypoint date echo-demo   # entrypoint overridden
```

## 7. Tagging, save/load, cleanup
```bash
docker tag hello-app:basic hello-app:latest
docker save -o hello-app.tar hello-app:basic      # export image to tar
docker rmi hello-app:basic && docker load -i hello-app.tar
docker container prune          # remove stopped containers
docker image prune              # remove dangling images
docker system df                # disk usage
```
