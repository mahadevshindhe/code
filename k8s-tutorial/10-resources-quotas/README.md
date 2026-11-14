# Module 10 - Resources: requests, limits, QoS, LimitRange, ResourceQuota

**Concepts:** requests (scheduling) vs limits (enforcement), CPU throttling vs memory OOMKill, QoS classes,
eviction, LimitRange defaults, ResourceQuota per namespace, JVM sizing in containers.

```bash
cd 10-resources-quotas
kubectl apply -f namespace-policies.yaml
kubectl -n quota-demo describe quota team-quota
kubectl -n quota-demo describe limitrange container-limits
```

## 1. QoS classes
```bash
kubectl apply -f qos-pods.yaml
kubectl -n quota-demo get pods -o custom-columns=NAME:.metadata.name,QOS:.status.qosClass
kubectl -n quota-demo get pod qos-besteffort -o jsonpath='{.spec.containers[0].resources}'   # defaults injected by LimitRange
```

## 2. Quota enforcement
```bash
kubectl -n quota-demo describe quota team-quota          # used vs hard
cat <<'YAML' | kubectl -n quota-demo apply -f -      # rejected: exceeds LimitRange max / quota
apiVersion: v1
kind: Pod
metadata: { name: big }
spec:
  containers:
    - { name: app, image: "nginx:1.27-alpine", resources: { requests: { cpu: "2" }, limits: { cpu: "2" } } }
YAML
kubectl -n quota-demo create deployment many --image=nginx:1.27-alpine --replicas=10
kubectl -n quota-demo get pods                           # only some pods are created (pods: "8")
kubectl -n quota-demo describe rs -l app=many | tail -5  # FailedCreate ... exceeded quota
kubectl -n quota-demo delete deployment many
```

## 3. Scheduling uses REQUESTS
```bash
cat <<'YAML' | kubectl apply -f -
apiVersion: v1
kind: Pod
metadata: { name: huge }
spec:
  containers:
    - { name: app, image: "nginx:1.27-alpine", resources: { requests: { memory: 100Gi } } }
YAML
kubectl get pod huge                                     # Pending
kubectl describe pod huge | grep -A3 Events              # 0/3 nodes: Insufficient memory
kubectl delete pod huge
kubectl describe nodes | grep -A8 "Allocated resources"  # per-node request/limit totals
```

## 4. Memory limit -> OOMKilled
```bash
kubectl apply -f oom-demo.yaml
kubectl -n quota-demo wait --for=condition=ready pod/oom-demo --timeout=120s
kubectl -n quota-demo port-forward pod/oom-demo 8080:8080 &
curl "localhost:8080/hello"                              # maxHeapMB ~ 190 (95% of 200Mi)
curl "localhost:8080/alloc?mb=150"                       # may OOM-kill the container (heap + metaspace + threads > 200Mi)
curl "localhost:8080/alloc?mb=400"
kubectl -n quota-demo get pod oom-demo                   # RESTARTS 1
kubectl -n quota-demo describe pod oom-demo | grep -B2 -A6 "Last State"   # Reason: OOMKilled, Exit Code 137
kill %1
```
Lower `MaxRAMPercentage` (50-75%) leaves room for non-heap memory.

## 5. CPU limit -> throttling (not kill)
```bash
kubectl -n quota-demo top pod                            # needs metrics-server
# a CPU-bound app at limit 500m simply runs slower (CFS throttling). Many teams set CPU requests, not CPU limits.
```

## Guidance
- **Requests** = what the scheduler reserves (size from p50/p90 usage). **Limits** = hard cap.
- Memory: set request == limit (predictable, avoids node-pressure evictions). CPU: set request, consider omitting limit.
- JVM: `-XX:MaxRAMPercentage=70`; note `availableProcessors()` derives from CPU limit.
- Namespaces: always apply LimitRange + ResourceQuota in shared clusters.
- Right-sizing tools: VPA (recommendation mode), Goldilocks, Kubecost.

## Cleanup
```bash
kubectl delete namespace quota-demo
```
