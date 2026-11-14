# Module 11 - Autoscaling (HPA) and metrics

**Concepts:** metrics-server, HorizontalPodAutoscaler v2, utilisation targets, scale behaviour/stabilisation,
`kubectl top`; VPA, Cluster Autoscaler and KEDA (conceptual).

Prereq: metrics-server (installed by `create-cluster.sh`). Check: `kubectl top nodes`.

```bash
cd 11-autoscaling-hpa
kubectl create namespace demo 2>/dev/null
kubectl apply -f deployment.yaml
kubectl apply -f hpa.yaml
kubectl -n demo get hpa -w             # TARGETS shows cpu: <current>/50%  (first value may be <unknown> for ~1 min)
```

## Generate load (another terminal)
```bash
kubectl apply -f load-generator.yaml          # 2 pods calling /burn continuously
kubectl -n demo get hpa -w                    # CPU rises, REPLICAS 1 -> 3 -> 5
kubectl -n demo get pods -w
kubectl -n demo top pods
```
Stop the load and watch scale-down (~1-2 min):
```bash
kubectl -n demo delete deployment load-generator
kubectl -n demo get hpa -w
kubectl -n demo describe hpa hello            # Events show every scaling decision and reason
```

## Imperative alternative
```bash
kubectl -n demo autoscale deployment hello --cpu-percent=50 --min=1 --max=5
```

## How HPA decides
`desiredReplicas = ceil(currentReplicas * currentMetric / targetMetric)` evaluated every 15s;
stabilisation windows prevent flapping.

## Other autoscalers (architect view)
| Tool | Scales | Notes |
|---|---|---|
| HPA | pod **replicas** | CPU/memory/custom/external metrics |
| VPA | pod **requests/limits** | recommendation or auto mode; don't combine with HPA on the same metric |
| Cluster Autoscaler / Karpenter | **nodes** | adds nodes when pods are Pending (cloud only) |
| KEDA | replicas from events | Kafka lag, queue length, cron, scale to zero |

Java tip: JVM warm-up makes CPU spikes at startup - use startup probes and sensible `stabilizationWindowSeconds`.

## Cleanup
```bash
kubectl delete namespace demo
```
