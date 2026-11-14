# Module 17 - Capstone: full Spring Boot + PostgreSQL + Redis stack on Kubernetes

Combines: Namespace, ConfigMap, Secret (file mount + secretKeyRef), StatefulSet + PVC, Deployments, Services,
Ingress, init container, probes, graceful shutdown, resources, securityContext, topology spread, HPA, PDB.

```
 browser -> Ingress (notes.localtest.me:8080) -> Service notes-app -> 2 x notes-app pods
                                                                  |-> Service postgres (headless) -> StatefulSet postgres-0 (PVC)
                                                                  '-> Service redis -> redis pod
```
Prereq: `./scripts/build-and-load-images.sh` (provides `notes-app:1.0` inside kind).

## Deploy
```bash
cd 17-full-stack-notes
kubectl apply -f 00-namespace-config.yaml
kubectl apply -f 10-postgres.yaml -f 20-redis.yaml
kubectl -n notes rollout status statefulset/postgres deployment/redis
kubectl apply -f 30-app.yaml
kubectl -n notes rollout status deployment/notes-app --timeout=300s
kubectl -n notes get all,ingress,pvc,hpa,pdb
```
(Order matters only for the namespace; everything else converges. `kubectl apply -f .` also works after a first run.)

## Use it
```bash
curl -X POST http://notes.localtest.me:8080/notes -H 'Content-Type: application/json' -d '{"text":"hello k8s"}'
curl -X POST http://notes.localtest.me:8080/notes -H 'Content-Type: application/json' -d '{"text":"second note"}'
curl http://notes.localtest.me:8080/notes
for i in 1 2 3 4; do curl -s http://notes.localtest.me:8080/visits; echo; done    # servedBy alternates between pods
curl http://notes.localtest.me:8080/actuator/health
```

## Experiments
1. **Self-healing:** `kubectl -n notes delete pod -l app=notes-app` -> new pods; requests keep working.
2. **DB persistence:** `kubectl -n notes delete pod postgres-0`; wait Ready; `curl .../notes` -> data still present.
3. **Secret wiring:** `kubectl -n notes exec deploy/notes-app -- ls /run/secrets` and check `/info`.
4. **Rolling update:** `kubectl -n notes set env deploy/notes-app EXTRA=1`; watch `kubectl -n notes get pods -w`.
5. **Scale / HPA:** `kubectl -n notes get hpa`; `kubectl -n notes scale deploy/notes-app --replicas=3` (HPA may reconcile).
6. **Failure of a dependency:** `kubectl -n notes scale deploy/redis --replicas=0` -> `/visits` fails; `kubectl -n notes scale deploy/redis --replicas=1` -> recovers.
7. **Inspect from inside Postgres:** `kubectl -n notes exec -it postgres-0 -- psql -U notes -d notes -c 'select * from notes;'`
8. **Network lock-down (Calico only):** `kubectl apply -f optional-networkpolicy.yaml`, then test from a throwaway pod:
   `kubectl -n notes run t --rm -it --image=busybox:1.36 --restart=Never -- nc -zv -w2 postgres 5432` -> blocked.
9. **Logs across pods:** `kubectl -n notes logs -l app=notes-app --prefix --tail=5`

## Production gaps (architect checklist)
| Here | Production |
|---|---|
| Plain Secret manifest | External Secrets / Vault / Sealed Secrets |
| Single Postgres pod | Operator (CloudNativePG) or managed DB |
| Redis w/o persistence | Redis Sentinel/Cluster or managed cache |
| `kubectl apply` | GitOps (Argo CD / Flux) + Helm/Kustomize (modules 18-19) |
| localtest.me / HTTP | cert-manager + real DNS + TLS |
| No policies | NetworkPolicies, PSA restricted, quotas |

## Cleanup
```bash
kubectl delete namespace notes
```
