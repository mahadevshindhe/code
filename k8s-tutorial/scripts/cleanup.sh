#!/usr/bin/env bash
# Removes everything the modules created (keeps the cluster). Use delete-cluster.sh to remove the cluster too.
for ns in dev staging demo quota-demo rbac-demo netpol-demo psa-demo notes kust-dev kust-prod crd-demo monitoring monitoring-demo; do
  kubectl delete namespace "$ns" --ignore-not-found --wait=false
done
kubectl delete pv --all --ignore-not-found
kubectl delete -f "$(dirname "$0")/../20-crd/crd.yaml" --ignore-not-found 2>/dev/null
kubectl delete clusterrole,clusterrolebinding -l tutorial=k8s --ignore-not-found
kubectl delete priorityclass -l tutorial=k8s --ignore-not-found
helm uninstall hello -n helm-demo 2>/dev/null; kubectl delete namespace helm-demo --ignore-not-found --wait=false
helm uninstall kps -n monitoring 2>/dev/null
kubectl uncordon --all 2>/dev/null
kubectl taint nodes --all dedicated- 2>/dev/null
echo "Namespaces being deleted in background. Check: kubectl get ns"
