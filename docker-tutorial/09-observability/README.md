# Module 09 - Observability (metrics, logs, health)

**Concepts:** Actuator + Micrometer, Prometheus DNS service discovery, Grafana provisioning,
`docker logs`, log drivers, `docker events`.

## Run
```bash
cd 09-observability
docker compose up -d --build
```
(`ports: "8080-8081:8080"` publishes each replica on a different host port.)

## Explore
- App metrics endpoint: http://localhost:8080/actuator/prometheus
- Prometheus: http://localhost:9090  -> Status > Targets (2 targets UP) -> query `jvm_memory_used_bytes`
- Grafana: http://localhost:3000 (admin/admin) -> Dashboards > Import -> ID **4701** (JVM Micrometer),
  data source *Prometheus*. (Import fetches the dashboard from grafana.com, needs internet.)

Generate load:
```bash
for i in $(seq 1 200); do curl -s localhost:8080/hello > /dev/null; curl -s localhost:8081/hello >/dev/null; done
curl "localhost:8080/burn?seconds=20"
```
Prometheus queries to try:
```
rate(http_server_requests_seconds_count[1m])
process_cpu_usage
jvm_memory_used_bytes{area="heap"}
```

## Logs
```bash
docker compose logs -f hello                 # aggregated from both replicas
docker logs --since 5m --tail 50 observability-hello-1
docker events --filter type=container        # live lifecycle events (another terminal: docker restart ...)
docker inspect --format '{{.HostConfig.LogConfig}}' observability-hello-1
```
Log rotation (add to a service to avoid disk fill-up):
```yaml
logging:
  driver: json-file
  options: { max-size: "10m", max-file: "3" }
```

## Cleanup
```bash
docker compose down -v
```
