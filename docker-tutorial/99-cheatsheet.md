# Docker Cheat Sheet (for Java architects)

## Images
| Command | Purpose |
|---|---|
| `docker build -t name:tag -f Dockerfile ctx` | build image |
| `docker images` / `docker rmi img` | list / remove |
| `docker history img` | layers |
| `docker tag src dst` / `docker push|pull` | registry ops |
| `docker save -o f.tar img` / `docker load -i f.tar` | offline transfer |
| `docker image prune -a` | remove unused images |

## Containers
| Command | Purpose |
|---|---|
| `docker run -d --name n -p 8080:8080 -e K=V img` | start |
| `docker ps -a` | list |
| `docker logs -f n` | logs |
| `docker exec -it n sh` | shell |
| `docker stop/start/restart/kill/rm -f n` | lifecycle |
| `docker inspect n` / `docker stats` / `docker top n` | introspection |
| `docker cp n:/path ./` | copy files |
| `docker run --rm ...` | auto-remove after exit |

## Volumes / Networks
`docker volume create|ls|inspect|rm|prune` - `docker network create|ls|inspect|connect|disconnect|rm`

## Compose v2
`docker compose up -d --build` - `down [-v]` - `ps` - `logs -f svc` - `exec svc sh` - `config` - `--profile x` - `-f a.yml -f b.yml` - `watch`

## Swarm
`docker swarm init` - `docker stack deploy -c f.yml name` - `docker service ls|ps|scale|update|rollback` - `docker secret create`

## Dockerfile instructions
`FROM` base - `ARG` build-time var - `ENV` runtime var - `WORKDIR` - `COPY/ADD` - `RUN` build step - `USER` -
`EXPOSE` doc only - `VOLUME` - `HEALTHCHECK` - `ENTRYPOINT` executable - `CMD` default args - `LABEL` - `STOPSIGNAL` - `SHELL`

## Exit codes
0 ok - 1 app error - 3 JVM ExitOnOutOfMemoryError - 137 SIGKILL/OOM - 143 SIGTERM

## Java-in-container rules
1. JRE/distroless runtime, multi-stage build  2. layered jar  3. `-XX:MaxRAMPercentage`  4. graceful shutdown
5. non-root + read-only fs  6. health endpoints (liveness/readiness)  7. config via env/files, secrets via files
8. log to stdout/stderr  9. one process per container  10. stateless app, state in volumes/DB
