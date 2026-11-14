# Module 20 - CustomResourceDefinitions & the Operator pattern

**Concepts:** extending the Kubernetes API, CRD schema validation/defaulting, printer columns, status subresource,
controllers/reconcile loop, ownerReferences & garbage collection, operators (Operator SDK, Kubebuilder,
Java Operator SDK / Quarkus Operator SDK).

```bash
cd 20-crd
kubectl apply -f crd.yaml
kubectl get crd | grep tutorial
kubectl api-resources | grep helloapp
kubectl explain helloapp.spec                    # your CRD schema is browsable like built-ins
```

## 1. Create custom resources (just data - nothing runs yet)
```bash
kubectl apply -f custom-resource.yaml
kubectl -n crd-demo get helloapps                 # printer columns; short name: kubectl get ha -A
kubectl -n crd-demo get ha greeter -o yaml        # note default values applied
kubectl -n crd-demo get deploy                    # none - no controller yet
```

## 2. Validation by the API server
```bash
kubectl apply -f invalid-resource.yaml            # rejected: spec.replicas must be <= 10, message too short
```

## 3. Run the toy controller (reconcile loop)
In a second terminal:
```bash
./controller.sh
```
Back in the first:
```bash
kubectl -n crd-demo get deploy,pods               # Deployment "greeter" appears with 2 replicas
kubectl -n crd-demo get ha                         # Ready column filled from status
kubectl -n crd-demo patch ha greeter --type=merge -p '{"spec":{"replicas":4}}'
kubectl -n crd-demo get pods -w                    # controller scales the Deployment within ~5s
kubectl -n crd-demo delete deploy greeter          # controller recreates it (self-healing at the operator level)
kubectl -n crd-demo delete ha greeter              # owned Deployment + pods are garbage-collected
```

## The reconcile loop (what every operator does)
```
watch custom resources  ->  compare DESIRED (spec) vs ACTUAL (cluster)  ->  create/update/delete child objects  ->  write status
```
Production operators add: informers/watches (no polling), leader election, finalizers (cleanup of external
resources), conditions in `.status`, retries with back-off, RBAC for the controller's ServiceAccount, webhooks.

## Why architects care
Databases (CloudNativePG, Strimzi, Percona), certificate management (cert-manager), GitOps (Argo CD `Application`),
service meshes, Crossplane (cloud resources as CRDs) - the platform *is* CRDs + controllers.

## Cleanup
```bash
kubectl delete -f custom-resource.yaml --ignore-not-found
kubectl delete -f crd.yaml                        # deletes all HelloApp objects too
```
