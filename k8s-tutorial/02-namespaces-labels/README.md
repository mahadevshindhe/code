# Module 02 - Namespaces, labels, selectors, annotations

**Concepts:** namespaces (logical isolation), labels (identity, used by selectors), annotations (metadata),
set-based selectors, field selectors, cross-namespace DNS, contexts.

```bash
cd 02-namespaces-labels
kubectl apply -f namespaces.yaml
kubectl apply -f labeled-pods.yaml
kubectl get ns --show-labels
```

## Namespaces
```bash
kubectl get pods -n dev
kubectl get pods -n staging
kubectl get pods -A                                # all namespaces
kubectl get pods                                   # 'default' namespace -> nothing

kubectl config set-context --current --namespace=dev    # change default namespace for kubectl
kubectl get pods
kubectl config set-context --current --namespace=default

kubectl create namespace scratch && kubectl delete namespace scratch   # deleting a namespace deletes EVERYTHING inside
```
Cluster-scoped resources (nodes, PVs, namespaces, ClusterRoles) are not namespaced: `kubectl api-resources --namespaced=false`.

## Labels & selectors
```bash
kubectl get pods -n dev --show-labels
kubectl get pods -n dev -l app=web                      # equality
kubectl get pods -n dev -l 'app=web,version=v2'         # AND
kubectl get pods -n dev -l 'version in (v1,v2),tier!=backend'     # set-based
kubectl get pods -n dev -l '!owner'                     # label does not exist
kubectl get pods -A -l app=web                          # across namespaces
kubectl get pods -n dev -L app,version                  # show labels as columns

kubectl label pod web-v1 -n dev release=stable          # add label
kubectl label pod web-v1 -n dev release=canary --overwrite
kubectl label pod web-v1 -n dev release-                # remove label
kubectl annotate pod web-v1 -n dev note="edited by me"
kubectl get pod web-v1 -n dev -o jsonpath='{.metadata.annotations}'
```
Field selectors: `kubectl get pods -A --field-selector status.phase=Running,metadata.namespace=dev`

> Labels drive **Services, Deployments (ReplicaSets), NetworkPolicies, affinity, HPA** - this is the core
> glue of Kubernetes. Annotations hold non-identifying data (owners, links, tool configuration such as ingress annotations).

## Recommended label keys
`app.kubernetes.io/name`, `.../instance`, `.../version`, `.../component`, `.../part-of`, `.../managed-by`.

## Cross-namespace DNS
`<service>.<namespace>.svc.cluster.local` (see module 04).

## Cleanup
```bash
kubectl delete -f . --ignore-not-found
```
