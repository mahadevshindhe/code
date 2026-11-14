#!/usr/bin/env bash
# Creates the local kind cluster + ingress-nginx + metrics-server.
# Usage: ./scripts/create-cluster.sh [--calico]    (--calico enables NetworkPolicy enforcement, module 15)
set -euo pipefail
cd "$(dirname "$0")"

CONFIG=kind-config.yaml
CALICO=false
if [[ "${1:-}" == "--calico" ]]; then CONFIG=kind-config-calico.yaml; CALICO=true; fi

command -v kind >/dev/null    || { echo "kind not found: https://kind.sigs.k8s.io/docs/user/quick-start/#installation"; exit 1; }
command -v kubectl >/dev/null || { echo "kubectl not found: https://kubernetes.io/docs/tasks/tools/"; exit 1; }
docker info >/dev/null 2>&1   || { echo "Docker daemon is not running"; exit 1; }

kind create cluster --config "$CONFIG"
kubectl config use-context kind-k8s-tutorial

if $CALICO; then
  echo ">> Installing Calico CNI"
  kubectl apply -f https://raw.githubusercontent.com/projectcalico/calico/v3.28.2/manifests/calico.yaml
  kubectl -n kube-system rollout status daemonset/calico-node --timeout=300s
fi
kubectl wait --for=condition=Ready nodes --all --timeout=300s

echo ">> Installing ingress-nginx"
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.11.3/deploy/static/provider/kind/deploy.yaml
kubectl wait --namespace ingress-nginx --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller --timeout=300s

echo ">> Installing metrics-server (needed for 'kubectl top' and HPA)"
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
kubectl patch -n kube-system deployment metrics-server --type=json \
  -p '[{"op":"add","path":"/spec/template/spec/containers/0/args/-","value":"--kubelet-insecure-tls"}]'

echo ">> Cluster ready:"
kubectl get nodes -o wide --show-labels | cut -c1-200
