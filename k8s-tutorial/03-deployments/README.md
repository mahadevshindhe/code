# Module 03 - ReplicaSets & Deployments (rolling updates, rollback, scaling, canary)

**Concepts:** Deployment -> ReplicaSet -> Pods, desired-state reconciliation (self-healing), scaling,
rolling update (maxSurge/maxUnavailable), rollout history & undo, Recreate strategy, pause/resume, canary.

```bash
cd 03-deployments
kubectl create namespace demo
kubectl apply -f deployment.yaml
kubectl -n demo get deploy,rs,pods -o wide
kubectl -n demo rollout status deployment/hello
```

## 1. Self-healing
```bash
kubectl -n demo get pods
kubectl -n demo delete pod <one-pod-name>
kubectl -n demo get pods -w              # ReplicaSet immediately creates a replacement
```

## 2. Scaling
```bash
kubectl -n demo scale deployment hello --replicas=5
kubectl -n demo get pods -o wide         # spread over worker nodes
kubectl -n demo scale deployment hello --replicas=3
```

## 3. See which pod answers (open 2nd terminal for watching)
```bash
kubectl -n demo port-forward svc/hello 8080:80 &
for i in $(seq 1 6); do curl -s localhost:8080/hello | grep -o '"pod":"[^"]*"\|"version":"[^"]*"'; done
```
> `port-forward` pins to ONE pod. For real load-balancing use another pod in the cluster:
> `kubectl -n demo run curl --rm -it --image=curlimages/curl --restart=Never -- sh -c 'for i in 1 2 3 4 5 6; do curl -s hello/hello; echo; done'`

## 4. Rolling update (zero downtime)
Terminal A - watch:
```bash
kubectl -n demo get pods -w
```
Terminal B - keep sending requests (run inside the cluster so it uses the Service):
```bash
kubectl -n demo run loadgen --rm -it --image=curlimages/curl --restart=Never -- \
  sh -c 'while true; do curl -s -o /dev/null -w "%{http_code} " hello/hello; sleep 0.3; done'
```
Terminal C - update image and env:
```bash
kubectl -n demo set image deployment/hello hello=hello-app:2.0
kubectl -n demo set env deployment/hello APP_VERSION=v2
kubectl -n demo rollout status deployment/hello
```
All responses stay `200` because `maxUnavailable: 0` + readiness probe.

## 5. History, rollback
```bash
kubectl -n demo rollout history deployment/hello
kubectl -n demo annotate deployment/hello kubernetes.io/change-cause="upgrade to 2.0" --overwrite
kubectl -n demo rollout history deployment/hello --revision=2
kubectl -n demo rollout undo deployment/hello                    # back to previous
kubectl -n demo rollout undo deployment/hello --to-revision=1
kubectl -n demo get rs                                           # old ReplicaSets kept (revisionHistoryLimit)
```

## 6. Bad release -> rollout stalls (readiness protects you)
```bash
kubectl -n demo set image deployment/hello hello=hello-app:does-not-exist
kubectl -n demo rollout status deployment/hello --timeout=30s     # waits... old pods still serving
kubectl -n demo get pods                                          # new pod: ErrImagePull / ImagePullBackOff
kubectl -n demo rollout undo deployment/hello
```

## 7. Pause / resume (batch several changes into one rollout)
```bash
kubectl -n demo rollout pause deployment/hello
kubectl -n demo set image deployment/hello hello=hello-app:2.0
kubectl -n demo set resources deployment/hello -c hello --limits=memory=600Mi
kubectl -n demo rollout resume deployment/hello
```

## 8. Recreate strategy
```bash
kubectl apply -f deployment-recreate.yaml
kubectl -n demo set env deployment/hello-recreate APP_VERSION=v2
kubectl -n demo get pods -l app=hello-recreate -w          # all terminate first, then new ones start
```

## 9. Canary release
```bash
kubectl apply -f canary.yaml
kubectl -n demo run curl --rm -it --image=curlimages/curl --restart=Never -- \
  sh -c 'for i in $(seq 1 20); do curl -s hello/hello | grep -o "\"version\":\"[^\"]*\""; done | sort | uniq -c'
# ~75% stable, ~25% canary.  Promote: update stable image, delete canary.  (Argo Rollouts / Flagger automate this.)
```

## Cleanup
```bash
kubectl delete namespace demo
```
