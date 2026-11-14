# Module 14 - RBAC & ServiceAccounts

**Concepts:** authentication vs authorisation, Role/ClusterRole, RoleBinding/ClusterRoleBinding,
ServiceAccount tokens, `kubectl auth can-i`, impersonation, least privilege, aggregated roles.

```bash
cd 14-rbac
kubectl apply -f rbac.yaml
kubectl -n rbac-demo get sa,role,rolebinding
```

## 1. Test permissions without logging in as that identity
```bash
kubectl auth can-i list pods   -n rbac-demo --as=system:serviceaccount:rbac-demo:pod-reader   # yes
kubectl auth can-i delete pods -n rbac-demo --as=system:serviceaccount:rbac-demo:pod-reader   # no
kubectl auth can-i list pods   -n default   --as=system:serviceaccount:rbac-demo:pod-reader   # no (Role is namespaced)
kubectl auth can-i list nodes               --as=system:serviceaccount:rbac-demo:pod-reader   # yes (ClusterRole)
kubectl auth can-i --list -n rbac-demo      --as=system:serviceaccount:rbac-demo:pod-reader
```

## 2. Call the API from inside a pod using the mounted token
```bash
kubectl -n rbac-demo wait --for=condition=ready pod/api-client pod/api-client-noperm
API=https://kubernetes.default.svc
CMD='TOKEN=$(cat /var/run/secrets/kubernetes.io/serviceaccount/token);
     CA=/var/run/secrets/kubernetes.io/serviceaccount/ca.crt;
     curl -s --cacert $CA -H "Authorization: Bearer $TOKEN" '

kubectl -n rbac-demo exec api-client -- sh -c "$CMD $API/api/v1/namespaces/rbac-demo/pods | head -15"       # works
kubectl -n rbac-demo exec api-client -- sh -c "$CMD $API/api/v1/namespaces/default/pods | head -15"         # 403 Forbidden
kubectl -n rbac-demo exec api-client-noperm -- sh -c "$CMD $API/api/v1/namespaces/rbac-demo/pods | head -15" # 403 Forbidden
```
The 403 message names the user `system:serviceaccount:rbac-demo:default` and the missing verb/resource - gold for debugging.

## 3. Quick imperative RBAC
```bash
kubectl create role cm-editor -n rbac-demo --verb=get,list,update --resource=configmaps
kubectl create rolebinding cm-editor-b -n rbac-demo --role=cm-editor --serviceaccount=rbac-demo:pod-reader
kubectl get clusterrole | head -20                              # built-in: cluster-admin, admin, edit, view
kubectl create rolebinding dev-view -n rbac-demo --clusterrole=view --user=jane@example.com
```

## 4. Short-lived token for a service account
```bash
kubectl -n rbac-demo create token pod-reader --duration=10m
```

## Key points
- RBAC is **additive** (no deny rules). No binding = no access.
- Humans authenticate via OIDC / cloud IAM (kubeconfig); pods use ServiceAccounts.
- Give every workload its own ServiceAccount; set `automountServiceAccountToken: false` when the app does not call the API.
- Avoid wildcard verbs/resources and `cluster-admin` bindings; review with `kubectl auth can-i --list`, `rbac-tool`, `kubectl-who-can`.

## Cleanup
```bash
kubectl delete -f rbac.yaml
```
