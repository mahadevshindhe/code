# Module 19 - Kustomize (template-free configuration management)

**Concepts:** base + overlays, namePrefix/namespace, replicas/images transformers, configMapGenerator with hash,
patches, `kubectl kustomize` / `apply -k`, diff before apply.

```
base/                     plain, valid manifests (deployment, service)
overlays/dev/             1 replica, namespace kust-dev, prefix dev-
overlays/prod/            3 replicas, image tag 2.0, bigger resources, namespace kust-prod, prefix prod-
```

```bash
cd 19-kustomize
kubectl create namespace kust-dev
kubectl create namespace kust-prod

kubectl kustomize overlays/dev            # render only (no cluster changes)
kubectl kustomize overlays/prod | grep -E "name:|replicas|image:|namespace"

kubectl diff -k overlays/prod || true     # what would change
kubectl apply -k overlays/dev
kubectl apply -k overlays/prod
kubectl get deploy,svc,cm -n kust-dev
kubectl get deploy,svc,cm -n kust-prod    # note generated ConfigMap name: prod-app-config-<hash>
```

## Verify
```bash
kubectl -n kust-dev port-forward svc/dev-hello 8081:80 &
kubectl -n kust-prod port-forward svc/prod-hello 8082:80 &
curl -s localhost:8081/hello      # "Hello from DEV overlay", version dev
curl -s localhost:8082/hello      # "Hello from PROD overlay", version prod
kill %1 %2
```

## Config change => automatic rollout
Edit `APP_GREETING` in `overlays/dev/kustomization.yaml`, then `kubectl apply -k overlays/dev`.
A new ConfigMap (new hash) is generated and the Deployment is updated to reference it -> rolling update.

## Handy extras
```bash
kubectl kustomize overlays/prod > /tmp/prod.yaml          # export rendered YAML (e.g. for review/GitOps)
kubectl delete -k overlays/dev
```
Other features: `components`, `secretGenerator`, `helmCharts` inflation, `replacements`, remote bases (`github.com/org/repo//path?ref=v1`).

## Cleanup
```bash
kubectl delete -k overlays/dev; kubectl delete -k overlays/prod
kubectl delete namespace kust-dev kust-prod
```
