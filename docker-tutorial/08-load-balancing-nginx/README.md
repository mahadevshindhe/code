# Module 08 - Scaling & load balancing with nginx

**Concepts:** replicas / `--scale`, Docker embedded DNS round-robin, reverse proxy, stateless services,
read-only bind mount of config, rolling replace.

## Run
```bash
cd 08-load-balancing-nginx
docker compose up -d --build
docker compose ps                     # 3 hello containers + nginx
```

## See load balancing (hostname = container ID changes)
```bash
for i in $(seq 1 9); do curl -s localhost:8080/hello | grep -o '"hostname":"[^"]*"'; done
```

## Scale at runtime
```bash
docker compose up -d --scale hello=5
sleep 6                                # nginx re-resolves DNS every 5s
for i in $(seq 1 10); do curl -s localhost:8080/hello | grep -o '"hostname":"[^"]*"'; done
docker compose up -d --scale hello=2
```

## Failure test
```bash
docker kill $(docker ps -q --filter name=lb-demo-hello | head -1)
for i in $(seq 1 6); do curl -s -o /dev/null -w "%{http_code}\n" localhost:8080/hello; done   # a few errors may appear until DNS refresh
```
> Lesson: simple DNS balancing has no health checks. Production setups use Traefik / Envoy /
> Kubernetes Services / cloud LBs that remove unhealthy backends automatically.

## Cleanup
```bash
docker compose down
```
