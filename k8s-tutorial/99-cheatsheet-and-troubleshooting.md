# kubectl Cheat Sheet & Troubleshooting Playbook

## Everyday commands
| Task | Command |
|---|---|
| List everything in a namespace | `kubectl get all,ingress,pvc,cm,secret -n <ns>` |
| Wide / YAML / JSONPath | `-o wide`, `-o yaml`, `-o jsonpath='{.items[*].metadata.name}'`, `-o custom-columns=...` |
| Watch | `kubectl get pods -w` |
| Describe (events!) | `kubectl describe <kind> <name>` |
| Logs | `kubectl logs <pod> -c <container> -f --previous --since=10m` |
| Shell | `kubectl exec -it <pod> -- sh` |
| Copy | `kubectl cp <ns>/<pod>:/path ./local` |
| Port-forward | `kubectl port-forward svc/<svc> 8080:80` |
| Apply / diff / delete | `kubectl apply -f x.yaml` / `kubectl diff -f x.yaml` / `kubectl delete -f x.yaml` |
| Edit live object | `kubectl edit deploy/<name>` |
| Patch | `kubectl patch deploy/<n> -p '{"spec":{"replicas":3}}'` |
| Rollouts | `kubectl rollout status|history|undo|restart deploy/<n>` |
| Scale | `kubectl scale deploy/<n> --replicas=5` |
| Resource usage | `kubectl top pods|nodes` |
| Who can do what | `kubectl auth can-i <verb> <resource> --as=<user>` |
| Docs | `kubectl explain <kind>.<field> --recursive` |
| Context / namespace | `kubectl config get-contexts`, `kubectl config set-context --current --namespace=<ns>` |
| Throwaway debug pod | `kubectl run dbg --rm -it --image=nicolaka/netshoot --restart=Never -- bash` |
| Ephemeral debug container | `kubectl debug -it <pod> --image=busybox:1.36 --target=<container>` |
| Debug a node | `kubectl debug node/<node> -it --image=busybox:1.36` |

## Symptom -> cause -> command
| Symptom | Likely cause | Check |
|---|---|---|
| `Pending` | no node fits (requests, taints, affinity, PVC unbound, quota) | `kubectl describe pod` -> Events (`FailedScheduling`) |
| `ErrImagePull` / `ImagePullBackOff` | wrong tag, private registry w/o secret, **image not loaded into kind** | `kubectl describe pod`; `kind load docker-image ...` |
| `CrashLoopBackOff` | app exits at start (config, DB unreachable, OOM) | `kubectl logs --previous`, exit code in `describe` |
| `OOMKilled` (exit 137) | memory limit too low / heap sized too large | `describe pod` -> Last State; lower `MaxRAMPercentage` or raise limit |
| `Running` but `0/1 Ready` | readiness probe failing | `describe pod` -> probe events; `kubectl logs` |
| Service has no endpoints | selector mismatch or pods not Ready | `kubectl get endpoints <svc>`; compare `svc.spec.selector` with pod labels |
| Ingress 404 / 503 | wrong host/path, backend has no endpoints | `kubectl describe ingress`; ingress-nginx controller logs |
| `connection refused` to service | app listens on 127.0.0.1 or wrong `targetPort` | `kubectl exec ... -- netstat -tln` / check port names |
| DNS fails | CoreDNS down, or NetworkPolicy blocks UDP/TCP 53 | `kubectl -n kube-system get pods -l k8s-app=kube-dns` |
| `CreateContainerConfigError` | missing ConfigMap/Secret/key | `kubectl describe pod` |
| `Forbidden` | RBAC | read message: user, verb, resource; `kubectl auth can-i` |
| Deployment rollout stuck | new pods unready, quota exceeded | `kubectl rollout status`, `kubectl describe rs` |
| PVC `Pending` | no StorageClass / waiting for first consumer | `kubectl describe pvc`, `kubectl get sc` |
| Node `NotReady` | kubelet/CNI problem, disk/memory pressure | `kubectl describe node`; (kind) `docker logs <node>` |

## Debug order that works
1. `kubectl get pods -o wide` (state, node, restarts)
2. `kubectl describe pod` (events at the bottom)
3. `kubectl logs [--previous]`
4. `kubectl get endpoints`, `kubectl exec` / netshoot to test connectivity (DNS -> IP -> port)
5. `kubectl get events --sort-by=.lastTimestamp -A`
6. Node/cluster level: `kubectl describe node`, `kubectl top`, component pods in `kube-system`

## Object quick reference
| Object | Purpose | Module |
|---|---|---|
| Pod | smallest unit; containers sharing net/volumes | 01 |
| Namespace / Label / Annotation | grouping & identity | 02 |
| ReplicaSet / Deployment | stateless replicas, rolling updates | 03 |
| Service / Endpoints | stable virtual IP + DNS + load balancing | 04 |
| ConfigMap / Secret | config & sensitive data | 05 |
| PV / PVC / StorageClass | persistent storage | 06 |
| StatefulSet | stable identity + per-pod storage | 07 |
| Ingress / IngressClass | L7 HTTP routing | 08 |
| Probes / lifecycle hooks | health & graceful shutdown | 09 |
| Requests/Limits / LimitRange / ResourceQuota | capacity governance | 10 |
| HPA (VPA, CA, KEDA) | autoscaling | 11 |
| Affinity / Taints / Spread / PDB / PriorityClass | scheduling & availability | 12 |
| Job / CronJob / DaemonSet | batch & per-node workloads | 13 |
| ServiceAccount / Role / RoleBinding | authorisation | 14 |
| NetworkPolicy | pod firewall | 15 |
| securityContext / Pod Security Admission | workload hardening | 16 |
| Helm / Kustomize | packaging & environment config | 18, 19 |
| CRD / Operator | extending Kubernetes | 20 |

## Java on Kubernetes - golden rules
1. One container per process; log to stdout/stderr (JSON logging recommended)
2. `-XX:MaxRAMPercentage=60-75`, memory request == limit, avoid tiny CPU limits
3. Actuator liveness/readiness + startup probe; liveness must not depend on external systems
4. Graceful shutdown: `server.shutdown=graceful`, preStop sleep, `terminationGracePeriodSeconds` > shutdown timeout
5. Externalise config (ConfigMap/Secret); secrets as files; never bake into images
6. Stateless pods, state in DB/cache/PVC; make startup idempotent (migrations: Flyway/Liquibase via Job or init)
7. Run as non-root with read-only root FS, drop capabilities
8. >= 2 replicas + PDB + topology spread; HPA on CPU or custom metrics
9. Expose `/actuator/prometheus`, add tracing (OpenTelemetry)
10. Use immutable image tags/digests; deploy via GitOps
