# Module 14 - Developer workflow: override files, hot rebuild, remote debugging, troubleshooting

**Concepts:** compose override files (`-f a -f b`), `docker compose watch`, JDWP remote debug from IntelliJ,
`docker run --rm`, `docker debug`/ephemeral debug containers, common failure diagnosis.

## 1. Normal vs debug mode (config layering)
```bash
cd 14-dev-workflow-debug
docker compose up -d --build                                   # normal
docker compose down
docker compose -f docker-compose.yml -f docker-compose.debug.yml up -d --build   # + debug port 5005
docker compose -f docker-compose.yml -f docker-compose.debug.yml config          # see merged result
```

## 2. Attach IntelliJ debugger
Run > Edit Configurations > **Remote JVM Debug** > Host `localhost`, Port `5005` > set a breakpoint in
`HelloController.hello()` > `curl localhost:8080/hello`.
(VS Code: launch config `{"type":"java","request":"attach","hostName":"localhost","port":5005}`.)

## 3. Rebuild automatically on code change
```bash
docker compose up --watch          # or: docker compose watch
# edit apps/hello-app/src/.../HelloController.java and save -> image rebuilt, container replaced
```

## 4. Troubleshooting playbook
| Symptom | Commands |
|---|---|
| Container exits immediately | `docker ps -a`, `docker logs <c>`, `docker inspect <c> --format '{{.State.ExitCode}} {{.State.Error}}'` |
| Exit code 137 | OOM kill or `docker kill` -> `docker inspect <c> --format '{{.State.OOMKilled}}'` |
| Exit code 143 | graceful SIGTERM (normal for `docker stop`) |
| Can't connect to port | `docker port <c>`; app must listen on `0.0.0.0` (not 127.0.0.1); check `-p` mapping |
| Name not resolving | containers must share a **user-defined** network: `docker network inspect <net>` |
| Need a shell in minimal image | `docker run --rm -it --network container:<c> --pid container:<c> nicolaka/netshoot` |
| Build is slow | `docker build --progress=plain .`, check `.dockerignore`, layer order |
| Disk full | `docker system df`, `docker system prune -a --volumes` (careful!) |
| What does compose really run | `docker compose config` |
| See events | `docker events` |

## 5. One-off tasks
```bash
docker run --rm -it -v "$(pwd)":/work -w /work maven:3.9-eclipse-temurin-21 mvn -v     # tools without installing them
docker run --rm -it eclipse-temurin:21-jdk jshell                                       # try Java instantly
```

## Cleanup
```bash
docker compose down
```
