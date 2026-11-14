# Module 12 - Docker Swarm (built-in orchestration)

**Concepts:** swarm init, services, tasks, replicas, routing mesh, rolling update, rollback,
secrets, node labels, desired-state reconciliation. (Kubernetes concepts map 1:1: Deployment, Service, Secret.)

## Prereq
Image `hello-app:layered` must exist (module 02).

## Run
```bash
docker swarm init                      # if on multiple NICs: --advertise-addr <ip>
cd 12-swarm
docker stack deploy -c stack.yml demo
docker stack services demo             # REPLICAS 3/3
docker service ps demo_hello           # one task per replica
for i in $(seq 1 6); do curl -s localhost:8080/hello | grep -o '"hostname":"[^"]*"'; done
```

## Self-healing (desired state)
```bash
docker kill $(docker ps -q --filter name=demo_hello | head -1)
docker service ps demo_hello           # a replacement task appears automatically
```

## Scale
```bash
docker service scale demo_hello=5
docker service scale demo_hello=2
```

## Rolling update & rollback
```bash
docker build -f ../02-multistage-builds/Dockerfile.layered -t hello-app:v2 ../apps/hello-app
docker service update --image hello-app:v2 --update-order start-first demo_hello
docker service ps demo_hello           # watch old tasks shut down one by one
docker service rollback demo_hello     # back to previous version
```

## Secrets
```bash
curl "localhost:8080/secret?name=db_password"
docker secret ls
docker service logs demo_hello
```

## Cleanup
```bash
docker stack rm demo
docker swarm leave --force
```
