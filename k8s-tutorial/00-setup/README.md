# Module 00 - Local cluster setup & kubectl basics

**Concepts:** cluster architecture (control plane vs nodes), kubectl, contexts, kind, image loading,
declarative vs imperative, API resources, `explain`, dry-run.

## Prerequisites
| Tool | Install |
|---|---|
| Docker Desktop / Docker Engine | https://docs.docker.com/get-docker/ |
| kubectl (v1.29+) | https://kubernetes.io/docs/tasks/tools/ |
| kind (v0.23+) | https://kind.sigs.k8s.io/docs/user/quick-start/ (`brew install kind`, `choco install kind`) |
| helm (v3) | https://helm.sh/docs/intro/install/ (modules 18, 21) |
| curl, bash | Windows: use WSL2 or Git Bash |

Give Docker at least **4 CPUs / 6 GB RAM** (Docker Desktop > Settings > Resources).

## 1. Create the cluster (from repository root)
```bash
./scripts/create-cluster.sh            # kind cluster "k8s-tutorial": 1 control-plane + 2 workers
                                       # + ingress-nginx + metrics-server
./scripts/build-and-load-images.sh     # builds hello-app:1.0/2.0 + notes-app:1.0 and loads them into kind
```
First run takes ~5-10 minutes (image pulls + Maven build).

> Module 15 (NetworkPolicy) needs a CNI that enforces policies. Create the cluster with
> `./scripts/create-cluster.sh --calico` instead (or `kind delete cluster --name k8s-tutorial` first, then recreate).

## 2. Verify
```bash
kubectl cluster-info
kubectl get nodes -o wide --show-labels
kubectl get pods -A                    # system pods: coredns, kube-proxy, kindnet, ingress-nginx, metrics-server
kubectl config get-contexts            # current context: kind-k8s-tutorial
```
Each kind node is a Docker container: `docker ps` shows `k8s-tutorial-control-plane`, `-worker`, `-worker2`.

## 3. kubectl essentials
```bash
kubectl api-resources                  # every resource type (pods, deployments, ...) with short names
kubectl explain pod.spec.containers    # built-in docs for any field   (add --recursive)
kubectl get pods -o wide | yaml | json | jsonpath='{.items[*].metadata.name}'
kubectl describe pod <name>            # details + EVENTS (first place to look when something fails)
kubectl apply -f file.yaml             # declarative create/update   (-f dir/ , -k kustomize dir)
kubectl delete -f file.yaml
kubectl diff -f file.yaml              # what WOULD change
kubectl apply -f file.yaml --dry-run=server -o yaml    # validate against API server
kubectl run tmp --image=busybox --restart=Never --dry-run=client -o yaml -- sleep 3600   # generate YAML skeleton
kubectl logs <pod> [-c container] [-f] [--previous]
kubectl exec -it <pod> -- sh
kubectl port-forward svc/<svc> 8080:80
```
Optional shell productivity: `alias k=kubectl` and `source <(kubectl completion bash)`.

## 4. Using minikube or Docker Desktop Kubernetes instead of kind
| Item | kind (default) | minikube | Docker Desktop K8s |
|---|---|---|---|
| Create | `create-cluster.sh` | `minikube start --nodes 3 --cni=calico` | Settings > Kubernetes > Enable |
| Load images | `kind load docker-image X` | `minikube image load X` | not needed (shares Docker) |
| Ingress | installed by script | `minikube addons enable ingress` | install ingress-nginx cloud manifest |
| metrics-server | installed by script | `minikube addons enable metrics-server` | install manually |
| LoadBalancer | stays Pending | `minikube tunnel` | works on localhost |
| Node labels (module 12) | set by kind config | `kubectl label node ...` manually | single node only |

Module commands assume kind; adapt the image loading and ingress URLs when using others.

## 5. Cleanup
```bash
./scripts/cleanup.sh          # remove module resources, keep cluster
./scripts/delete-cluster.sh   # delete the whole cluster
```
