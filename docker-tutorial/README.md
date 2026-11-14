# Docker Tutorial for Java Solution Architects

A hands-on, runnable tour of Docker using two small **Spring Boot 3 / Java 21** apps.
Every module has its own `README.md` with copy-paste commands and things to observe.

## Prerequisites
| Tool | Version | Needed for |
|---|---|---|
| Docker Engine / Docker Desktop | 24+ (with **Compose v2** and BuildKit) | everything |
| JDK 21 + Maven 3.9 | optional | modules 01, 11, 16 only (other modules build inside Docker) |
| curl, bash | any | commands (Windows: use WSL2 or Git Bash; PowerShell notes are in the READMEs) |

Check: `docker version && docker compose version`

Internet access is needed to pull base images and Maven dependencies. First builds take a few minutes.

## Quick start
```bash
unzip docker-tutorial.zip && cd docker-tutorial
./scripts/build-all-images.sh          # optional pre-build (Docker only, no Java needed)
cd 05-compose-full-stack && docker compose up -d --build
curl localhost:8080/visits
```
Run all commands **from the folder stated at the top of each module README** (most Dockerfile builds
run from the repository root, compose modules run inside their own folder).

## Project layout
```
docker-tutorial/
├── apps/
│   ├── hello-app/        Stateless REST app (+Actuator, Prometheus, Jib config) - used by most modules
│   └── notes-app/        REST + PostgreSQL + Redis (+Dockerfile, Testcontainers test)
├── 01-basics/                  image, container, run/exec/logs, ENTRYPOINT vs CMD, restart policy
├── 02-multistage-builds/       multi-stage, layered jars, distroless, cache mounts, healthcheck
├── 03-volumes-and-storage/     named volumes, bind mounts, tmpfs, backup/restore
├── 04-networking/              bridge, user-defined networks, DNS, aliases, isolation
├── 05-compose-full-stack/      Spring Boot + Postgres + Redis, healthchecks, profiles, tiers
├── 06-config-and-secrets/      env, env_file, configs, secrets (+ Spring configtree)
├── 07-resource-limits/         CPU/memory limits, JVM container awareness, OOM behaviour
├── 08-load-balancing-nginx/    replicas, scaling, nginx reverse proxy
├── 09-observability/           Prometheus + Grafana + logs/events
├── 10-local-registry/          private registry, tags, digests, push/pull
├── 11-jib-no-dockerfile/       Jib & Spring Boot buildpacks
├── 12-swarm/                   orchestration: stack, rolling update, rollback, secrets
├── 13-security-hardening/      non-root, read-only, cap-drop, scanning, checklist
├── 14-dev-workflow-debug/      override files, compose watch, JDWP debugging, troubleshooting
├── 15-buildx-multiarch/        amd64 + arm64 images, build cache, build secrets
├── 16-testcontainers/          Docker-powered Java integration tests
├── 99-cheatsheet.md
└── scripts/                    build-jars.sh, build-all-images.sh, cleanup.sh
```

## Concept coverage map
| Docker concept | Module |
|---|---|
| Images, layers, tags, containers, lifecycle | 01 |
| Dockerfile instructions (FROM, ARG, ENV, COPY, USER, HEALTHCHECK, ENTRYPOINT/CMD...) | 01, 02 |
| Multi-stage builds, build cache, BuildKit, .dockerignore, distroless | 02 |
| Volumes, bind mounts, tmpfs | 03 |
| Networks, DNS, port publishing, isolation | 04, 05 |
| Docker Compose (services, profiles, healthchecks, depends_on, override files) | 05, 06, 08, 14 |
| Env vars, configs, secrets | 06, 12 |
| Resource limits, cgroups, JVM tuning | 07 |
| Scaling & load balancing | 08, 12 |
| Logging, metrics, monitoring, events | 09 |
| Registries, digests, tagging strategy | 10 |
| Daemonless / buildpack builds | 11 |
| Orchestration (Swarm; maps to Kubernetes) | 12 |
| Security hardening, scanning, SBOM | 13 |
| Debugging, dev loop, troubleshooting | 14 |
| Multi-arch builds, remote cache, build secrets | 15 |
| Testing with containers | 16 |

## Suggested learning path
1 -> 2 -> 3 -> 4 -> 5 -> 6 -> 7 -> 8 -> 9 (core, ~half a day) then 10-16 as needed.

## Port map (avoid conflicts - run one compose module at a time)
8080 apps - 8081/8082 module 07 - 8090 Adminer - 5000/5001 registry - 9090 Prometheus - 3000 Grafana - 5005 JDWP.
If a port is busy: `docker compose down` in the previous module, or edit the host side of `ports:`.

## Cleanup
```bash
./scripts/cleanup.sh        # removes tutorial containers, volumes, images
docker system df            # check disk usage
```

## Notes
- The sample secret in `06-config-and-secrets/secrets/` is for demo only.
- Tested layout targets Docker Compose v2 syntax (`docker compose`, not `docker-compose`).
