# Module 18 - Helm (package manager & templating)

**Concepts:** chart structure, values, templates, helpers, releases, upgrade/rollback/history,
environment-specific values files, hooks/tests, `helm template/lint`, config checksum rollout, repos.

Prereq: Helm v3 (`helm version`) and images loaded (`build-and-load-images.sh`).

```
hello-chart/
  Chart.yaml            metadata (chart version, appVersion)
  values.yaml           defaults        values-dev.yaml / values-prod.yaml = env overrides
  templates/            Go-templated manifests (+ _helpers.tpl, NOTES.txt, tests/)
```

```bash
cd 18-helm
helm lint hello-chart
helm template hello hello-chart | less                  # render locally, nothing installed
helm template hello hello-chart -f hello-chart/values-prod.yaml | grep -E "replicas|image:|kind:"
```

## 1. Install
```bash
kubectl create namespace helm-demo
helm install hello hello-chart -n helm-demo -f hello-chart/values-dev.yaml
helm list -n helm-demo
kubectl -n helm-demo get all,ingress
curl http://hello-dev.localtest.me:8080/hello                 # "Hello from DEV"
helm test hello -n helm-demo                                  # runs templates/tests pod
```

## 2. Upgrade with different values
```bash
helm upgrade hello hello-chart -n helm-demo -f hello-chart/values-prod.yaml
kubectl -n helm-demo get pods                                  # 3 replicas, image 2.0, HPA enabled
helm upgrade hello hello-chart -n helm-demo -f hello-chart/values-dev.yaml --set greeting="Set from CLI" --set replicaCount=2
helm get values hello -n helm-demo                             # user-supplied values only
helm get manifest hello -n helm-demo | head -30                # what was applied
```
`--set` beats `-f` file beats chart defaults. Use `--atomic --wait --timeout 3m` in CI so failed upgrades auto-roll back.

## 3. History & rollback
```bash
helm history hello -n helm-demo
helm rollback hello 1 -n helm-demo
helm history hello -n helm-demo                                # rollback creates a NEW revision
```

## 4. Config checksum annotation
Change only `greeting` and upgrade -> pods restart automatically because `checksum/config` changed
(plain ConfigMap changes would not restart pods).

## 5. Debugging templates
```bash
helm template hello hello-chart --debug -s templates/deployment.yaml
helm upgrade --install hello hello-chart -n helm-demo --dry-run --debug
```

## 6. Using public charts (repos)
```bash
helm repo add bitnami https://charts.bitnami.com/bitnami && helm repo update
helm search repo postgresql
helm show values bitnami/postgresql | head -50
# helm install pg bitnami/postgresql -n helm-demo --set auth.postgresPassword=secret
```
Package/publish your chart: `helm package hello-chart` -> `.tgz` -> OCI registry (`helm push hello-chart-0.1.0.tgz oci://localhost:5000/charts`).

## Helm vs Kustomize (module 19)
Helm: templating + packaging + releases/rollback + public ecosystem. Kustomize: template-free overlays, built into kubectl.
Many teams use both (Helm for third-party, Kustomize or Helm values for own apps) with Argo CD / Flux.

## Cleanup
```bash
helm uninstall hello -n helm-demo
kubectl delete namespace helm-demo
```
