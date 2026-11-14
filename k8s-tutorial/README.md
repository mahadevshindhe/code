# Kubernetes Tutorial for Java Solution Architects

A hands-on tour of Kubernetes that runs **entirely on your laptop** using a local `kind` cluster
(1 control-plane + 2 workers, with Ingress and metrics-server) and two small **Spring Boot 3 / Java 21** apps.
Every module has its own `README.md` with commands to run and things to observe.

## Prerequisites
| Tool | Version | Notes |
|---|---|---|
| Docker Desktop / Engine | 24+ | give it >= 4 CPUs and 6 GB RAM |
| kubectl | 1.29+ | |
| kind | 0.23+ | local multi-node cluster in Docker |
| helm | 3.x | modules 18 and 21 |
| bash, curl, openssl | any | Windows: use WSL2 or Git Bash |
| JDK / Maven | not needed | images are built inside Docker |

Alternatives: minikube or Docker Desktop Kubernetes (see `00-setup/README.md` for differences).

## Quick start
```bash
unzip k8s-tutorial.zip && cd k8s-tutorial
./scripts/create-cluster.sh              # kind cluster + ingress-nginx + metrics-server   (~5 min)
./scripts/build-and-load-images.sh       # builds hello-app / notes-app and loads them into kind (~5 min)
cd 17-full-stack-notes                   # try the capstone first if you like
kubectl apply -f 00-namespace-config.yaml -f 10-postgres.yaml -f 20-redis.yaml
kubectl apply -f 30-app.yaml
curl http://notes.localtest.me:8080/visits
```
`*.localtest.me` resolves to 127.0.0.1 via public DNS, so Ingress hosts need no `/etc/hosts` edits.
Host ports used: **8080** (Ingress HTTP), **8443** (HTTPS), **30080** (NodePort demo).

## Modules
| # | Folder | Concepts |
|---|---|---|
| 00 | `00-setup` | cluster architecture, kind, kubectl, contexts, image loading, alternatives |
| 01 | `01-pods` | pods, multi-container/sidecar, init containers, Downward API, restart policy, CrashLoop debugging |
| 02 | `02-namespaces-labels` | namespaces, labels, selectors, annotations, field selectors |
| 03 | `03-deployments` | ReplicaSet, Deployment, rolling update, rollback, Recreate, pause/resume, canary |
| 04 | `04-services` | ClusterIP, NodePort, LoadBalancer, Headless, ExternalName, DNS, endpoints |
| 05 | `05-configmaps-secrets` | env vs file injection, update propagation, immutability, base64 vs encryption |
| 06 | `06-storage` | emptyDir, PV, PVC, StorageClass, dynamic/static provisioning, reclaim policy, access modes |
| 07 | `07-statefulsets` | stable identity, ordered rollout, volumeClaimTemplates, Postgres, partitioned updates |
| 08 | `08-ingress` | ingress-nginx, host/path routing, TLS, annotations |
| 09 | `09-probes-lifecycle` | startup/liveness/readiness probes, preStop, graceful shutdown, zero-downtime rollout |
| 10 | `10-resources-quotas` | requests/limits, QoS, OOMKilled, LimitRange, ResourceQuota, JVM sizing |
| 11 | `11-autoscaling-hpa` | metrics-server, HPA v2, behaviour, VPA/Cluster Autoscaler/KEDA overview |
| 12 | `12-scheduling` | nodeSelector, affinity/anti-affinity, taints, topology spread, PDB, priority, drain |
| 13 | `13-jobs-cronjobs-daemonsets` | Job, indexed/parallel jobs, CronJob, DaemonSet |
| 14 | `14-rbac` | ServiceAccount, Role/ClusterRole, bindings, `auth can-i`, API access from pods |
| 15 | `15-network-policies` | default-deny, ingress/egress rules, DNS gotcha (needs Calico) |
| 16 | `16-security-context` | securityContext, Pod Security Admission, hardened Java deployment |
| 17 | `17-full-stack-notes` | **capstone:** Spring Boot + PostgreSQL + Redis with everything combined |
| 18 | `18-helm` | chart, values, templates, releases, upgrade/rollback, tests |
| 19 | `19-kustomize` | base/overlays, generators, patches, images/replicas transformers |
| 20 | `20-crd` | CRD, schema validation, custom resources, operator pattern with a toy controller |
| 21 | `21-observability` | logs, events, kubectl top, Prometheus Operator, ServiceMonitor, Grafana |
| 99 | `99-cheatsheet-and-troubleshooting.md` | kubectl cheat sheet, symptom->cause table, Java-on-K8s rules |

## Suggested path
Core (about one day): 00 -> 01 -> 03 -> 04 -> 05 -> 06 -> 07 -> 08 -> 09 -> 10 -> 11 -> **17**.
Then 02, 12-16 (operations & security), 18-21 (delivery & platform).

## Project layout
```
k8s-tutorial/
├── apps/
│   ├── hello-app/    stateless Spring Boot app: pod/node info, probes switches, CPU/memory load, config/secret readers
│   └── notes-app/    Spring Boot + PostgreSQL + Redis (+ Testcontainers test)
├── scripts/          create-cluster.sh, build-and-load-images.sh, cleanup.sh, delete-cluster.sh, kind configs
├── 00-setup ... 21-observability   (each: YAML manifests + README.md)
└── 99-cheatsheet-and-troubleshooting.md
```

## Important local-cluster notes
- **Images:** kind nodes cannot see your local Docker images. Always run `./scripts/build-and-load-images.sh`
  (and re-run after code changes), and keep `imagePullPolicy: IfNotPresent` (already set in all manifests).
  `ErrImagePull` on `hello-app`/`notes-app` almost always means images were not loaded.
- **Network policies (module 15):** create the cluster with `./scripts/create-cluster.sh --calico`.
- **LoadBalancer services** stay `<pending>` on kind (see module 04 for options).
- **One module at a time:** most modules use the `demo` namespace; clean up with the module's cleanup step or `./scripts/cleanup.sh`.
- **Resource use:** the whole cluster needs ~3-4 GB RAM; module 21 (Prometheus stack) adds ~1.5 GB.

## Cleanup
```bash
./scripts/cleanup.sh          # delete tutorial namespaces and cluster-scoped leftovers (keeps the cluster)
./scripts/delete-cluster.sh   # remove the kind cluster entirely
```
