# Module 12 - Scheduling: affinity, taints, spread, PDB, priority, drain

**Concepts:** kube-scheduler (filter + score), nodeSelector, node/pod affinity & anti-affinity,
taints & tolerations, topology spread constraints, PodDisruptionBudget, PriorityClass/preemption,
cordon/drain.

Node names in this cluster: `k8s-tutorial-worker` (disktype=ssd, zone-a), `k8s-tutorial-worker2` (hdd, zone-b).

```bash
cd 12-scheduling
kubectl create namespace demo 2>/dev/null
kubectl get nodes --show-labels | grep -E "disktype|zone"
```

## 1. nodeSelector / node affinity
```bash
kubectl apply -f nodeselector-affinity.yaml
kubectl -n demo get pods -o wide                   # on-ssd + affinity-pod on worker (ssd); 'impossible' Pending
kubectl -n demo describe pod impossible | grep -A3 Events    # "didn't match Pod's node affinity/selector"
```

## 2. Pod anti-affinity (spread across nodes)
```bash
kubectl apply -f pod-anti-affinity.yaml
kubectl -n demo get pods -l app=spread -o wide     # distributed over worker nodes where possible
```

## 3. Topology spread across zones
```bash
kubectl apply -f topology-spread.yaml
kubectl -n demo get pods -l app=zone-spread -o wide     # 2 on zone-a node, 2 on zone-b node
```

## 4. Taints & tolerations
```bash
kubectl taint nodes k8s-tutorial-worker dedicated=gpu:NoSchedule
kubectl apply -f taints-tolerations.yaml
kubectl -n demo get pods tolerant intolerant -o wide     # tolerant runs on tainted node; intolerant Pending
kubectl describe node k8s-tutorial-worker | grep Taints
kubectl taint nodes k8s-tutorial-worker dedicated=gpu:NoSchedule-     # remove taint (trailing '-')
```
Effects: `NoSchedule` (don't place), `PreferNoSchedule` (try not to), `NoExecute` (also evict running pods).
The control-plane node has `node-role.kubernetes.io/control-plane:NoSchedule` by default.

## 5. PDB + drain (node maintenance)
```bash
kubectl apply -f pdb-priority.yaml
kubectl -n demo get pdb                                   # ALLOWED DISRUPTIONS 1
kubectl drain k8s-tutorial-worker2 --ignore-daemonsets --delete-emptydir-data   # evicts pods; honours PDB (may wait/refuse)
kubectl get nodes                                         # worker2: Ready,SchedulingDisabled
kubectl -n demo get pods -o wide                          # pods moved to other node
kubectl uncordon k8s-tutorial-worker2                     # make schedulable again
```
`kubectl cordon <node>` only marks unschedulable (no eviction).
> With only 2 workers and strict PDBs a drain can block - that is the PDB protecting availability.

## 6. Priority & preemption
```bash
kubectl get priorityclass
kubectl -n demo get pod -l app=critical -o custom-columns=NAME:.metadata.name,PRIORITY:.spec.priority
```
Built-in: `system-cluster-critical`, `system-node-critical`. When the cluster is full, pending high-priority pods evict lower ones.

## Architect's checklist for HA
1. >= 2-3 replicas + PDB  2. spread across nodes **and** zones  3. requests set (scheduler needs them)
4. readiness probes  5. priority for critical services  6. tolerations only for dedicated pools

## Cleanup
```bash
kubectl taint nodes --all dedicated- 2>/dev/null; kubectl uncordon --all
kubectl delete namespace demo; kubectl delete priorityclass business-critical
```
