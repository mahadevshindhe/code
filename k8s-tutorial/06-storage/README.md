# Module 06 - Storage: volumes, PV, PVC, StorageClass

**Concepts:** emptyDir, hostPath, PersistentVolume, PersistentVolumeClaim, StorageClass, dynamic vs static
provisioning, access modes, reclaim policy, volumeBindingMode, volume expansion, fsGroup.

```bash
cd 06-storage
kubectl create namespace demo 2>/dev/null
kubectl get storageclass                      # "standard (default)" provisioner rancher.io/local-path
```

## 1. Dynamic provisioning + persistence
```bash
kubectl apply -f pvc-dynamic.yaml
kubectl -n demo get pvc                       # Pending until a pod uses it (WaitForFirstConsumer)
kubectl apply -f deployment-pvc.yaml
kubectl -n demo rollout status deploy/hello-pvc
kubectl -n demo get pvc,pv                    # Bound; PV was created automatically
kubectl -n demo port-forward deploy/hello-pvc 8080:8080 &
curl localhost:8080/visit; curl localhost:8080/visit     # visits 1, 2
kill %1

kubectl -n demo delete pod -l app=hello-pvc  # pod dies, Deployment recreates it ...
kubectl -n demo rollout status deploy/hello-pvc
kubectl -n demo port-forward deploy/hello-pvc 8080:8080 &
curl localhost:8080/visit                     # visits: 3  -> data survived!
kill %1
```

## 2. Static provisioning
```bash
kubectl apply -f pv-static.yaml
kubectl get pv                                # manual-pv: Bound to demo/manual-claim
kubectl -n demo logs static-pv-pod
kubectl -n demo delete pod static-pv-pod && kubectl -n demo apply -f pv-static.yaml   # recreate pod -> log has 2 lines
kubectl -n demo logs static-pv-pod
```

## 3. Reclaim policy
```bash
kubectl -n demo delete pod static-pv-pod
kubectl -n demo delete pvc manual-claim
kubectl get pv                                # manual-pv: STATUS Released (Retain keeps data; admin must clean up)
```

## 4. Custom StorageClass & expansion
```bash
kubectl apply -f storageclass-custom.yaml
kubectl get sc
kubectl -n demo patch pvc hello-data -p '{"spec":{"resources":{"requests":{"storage":"2Gi"}}}}'   # expansion (needs class support)
```
> kind's local-path provisioner does not support online expansion - the command illustrates the API;
> on cloud classes (EBS gp3, Azure Disk, GCE PD) it works.

## 5. Access modes
| Mode | Meaning | Typical backends |
|---|---|---|
| RWO | read-write by one **node** | EBS, GCE PD, local-path |
| RWOP | read-write by one **pod** | CSI drivers |
| ROX | read-only by many nodes | NFS, CephFS |
| RWX | read-write by many nodes | NFS, EFS, CephFS, Azure Files |

## 6. Key rules
- Databases: use StatefulSet + `volumeClaimTemplates` (module 07), not a Deployment with one shared PVC.
- Deleting a PVC deletes the PV *and data* when reclaimPolicy is `Delete` (the default for dynamic classes).
- PVCs are namespaced; PVs are cluster-scoped. Snapshots: `VolumeSnapshot` (CSI snapshotter) - backups: Velero.
- `emptyDir` is node-local scratch space tied to pod lifetime (`medium: Memory` makes it tmpfs).

## Cleanup
```bash
kubectl delete namespace demo
kubectl delete pv manual-pv --ignore-not-found
kubectl delete sc fast-retain --ignore-not-found
```
