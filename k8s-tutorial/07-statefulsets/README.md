# Module 07 - StatefulSets & headless services

**Concepts:** stable network identity, ordered deployment/termination, `volumeClaimTemplates`,
PVC retention on delete, headless Service DNS, partitioned rolling updates, when NOT to run a DB in K8s.

```bash
cd 07-statefulsets
kubectl create namespace demo 2>/dev/null
kubectl apply -f statefulset-web.yaml
kubectl -n demo get pods -l app=web -w        # web-0 -> web-1 -> web-2 created IN ORDER (each waits for previous Ready)
kubectl -n demo get pvc                       # data-web-0, data-web-1, data-web-2
```

## 1. Stable identity & DNS
```bash
kubectl -n demo run dns --rm -it --image=busybox:1.36 --restart=Never -- sh -c '
  nslookup web;                 # headless service -> 3 pod IPs
  nslookup web-0.web;           # stable per-pod DNS: <pod>.<headless-svc>
  wget -qO- web-1.web;          # reach a SPECIFIC replica'
```
Pattern: `<pod-name>.<service>.<namespace>.svc.cluster.local` - how DB replicas / Kafka / ZooKeeper find peers.

## 2. Identity survives pod deletion; data survives too
```bash
kubectl -n demo exec web-1 -- sh -c 'echo persisted > /var/cache/nginx/marker'
kubectl -n demo delete pod web-1
kubectl -n demo get pods -w                    # web-1 comes back with the SAME name
kubectl -n demo exec web-1 -- cat /var/cache/nginx/marker     # same PVC re-attached -> "persisted"
```

## 3. Scaling is ordered
```bash
kubectl -n demo scale statefulset web --replicas=5     # creates web-3, web-4
kubectl -n demo scale statefulset web --replicas=2     # deletes web-4, web-3, web-2 (reverse order)
kubectl -n demo get pvc                                # PVCs REMAIN (data protection) - delete manually if unwanted
```

## 4. Staged rolling update (canary by ordinal)
```bash
kubectl -n demo patch statefulset web -p '{"spec":{"updateStrategy":{"type":"RollingUpdate","rollingUpdate":{"partition":1}}}}'
kubectl -n demo set image statefulset/web nginx=nginx:1.27.2-alpine
kubectl -n demo get pods -o custom-columns=NAME:.metadata.name,IMAGE:.spec.containers[0].image
# only ordinals >= partition (web-1) updated; set partition to 0 to finish
```

## 5. PostgreSQL example
```bash
kubectl apply -f postgres.yaml
kubectl -n demo wait --for=condition=ready pod/postgres-0 --timeout=120s
kubectl -n demo exec -it postgres-0 -- psql -U demo -d demo -c "create table t(x int); insert into t values (42); select * from t;"
kubectl -n demo delete pod postgres-0
kubectl -n demo wait --for=condition=ready pod/postgres-0 --timeout=120s
kubectl -n demo exec postgres-0 -- psql -U demo -d demo -c "select * from t;"     # data still there
```

## Architect's notes
- StatefulSet != HA database. Replication/failover logic is your job -> use an **Operator**
  (CloudNativePG, Zalando Postgres Operator, Strimzi for Kafka) or a managed service (RDS, Cloud SQL).
- Use `podAntiAffinity` / topology spread so replicas land on different nodes/zones (module 12).
- `persistentVolumeClaimRetentionPolicy` (K8s 1.27+) can auto-delete PVCs on scale-down/delete.

## Cleanup
```bash
kubectl -n demo delete statefulset --all; kubectl -n demo delete pvc --all
kubectl delete namespace demo
```
