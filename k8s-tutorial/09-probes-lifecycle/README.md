# Module 09 - Probes, lifecycle hooks, graceful shutdown

**Concepts:** startup / liveness / readiness probes (HTTP, TCP, exec, gRPC), restart vs removal from endpoints,
Spring Boot health groups, preStop hook, SIGTERM + `terminationGracePeriodSeconds`, graceful shutdown.

| Probe | Question | On failure |
|---|---|---|
| startup | has the app finished starting? | container restarted; other probes disabled until success |
| liveness | is it stuck/dead? | **container restarted** |
| readiness | can it take traffic now? | **removed from Service endpoints** |

```bash
cd 09-probes-lifecycle
kubectl create namespace demo 2>/dev/null
kubectl apply -f deployment.yaml
kubectl -n demo rollout status deploy/hello
kubectl -n demo get pods
```

## 1. Readiness failure -> traffic stops, pod stays
```bash
POD=$(kubectl -n demo get pod -l app=hello -o jsonpath='{.items[0].metadata.name}')
kubectl -n demo get endpoints hello                                   # 2 IPs
kubectl -n demo port-forward pod/$POD 9090:8080 &
curl localhost:9090/admin/unready                                     # app flips its own readiness state
sleep 8
kubectl -n demo get pods                                              # READY 0/1 but NOT restarted
kubectl -n demo get endpoints hello                                   # only 1 IP now
curl localhost:9090/admin/ready ; sleep 8; kubectl -n demo get endpoints hello    # back to 2
```

## 2. Liveness failure -> container restart
```bash
curl localhost:9090/admin/unhealthy
sleep 20
kubectl -n demo get pods                                              # RESTARTS = 1
kubectl -n demo describe pod $POD | grep -A6 "Events:"                # "Liveness probe failed ... will be restarted"
kill %1
```

## 3. Exec-probe demo
```bash
kubectl apply -f bad-liveness.yaml
kubectl -n demo get pod liveness-exec -w                              # restarts every ~35s
kubectl -n demo describe pod liveness-exec | tail -15
```

## 4. Graceful shutdown (rolling update without dropped requests)
Shell 1:
```bash
kubectl -n demo run loadgen --rm -it --image=curlimages/curl --restart=Never -- \
  sh -c 'while true; do curl -s -o /dev/null -w "%{http_code} " --max-time 5 hello/hello; sleep 0.2; done'
```
Shell 2:
```bash
kubectl -n demo rollout restart deployment/hello
kubectl -n demo get pods -w
```
Output stays `200 200 200 ...`. Why: new pods become Ready first (readiness), old pods get
`preStop sleep 5` (endpoint removal propagates), then SIGTERM -> Spring Boot `server.shutdown=graceful`
finishes in-flight requests -> exit before `terminationGracePeriodSeconds`.

Try removing the `preStop` block and re-running to see occasional connection errors.

## 5. Termination timeline
```
delete pod -> [Terminating] + removed from endpoints (async!) -> preStop hook -> SIGTERM -> (grace period) -> SIGKILL
```

## Rules of thumb
- Liveness must NOT check dependencies (DB down => restarting your pods makes things worse). Readiness may.
- Use a startup probe instead of a huge `initialDelaySeconds`.
- Spring Boot: enable `management.endpoint.health.probes.enabled=true` -> `/actuator/health/liveness|readiness`.
- Keep `spring.lifecycle.timeout-per-shutdown-phase` < `terminationGracePeriodSeconds`.

## Cleanup
```bash
kubectl delete namespace demo
```
