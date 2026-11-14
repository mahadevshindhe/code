# Module 01 - Pods

**Concepts:** Pod = smallest deployable unit; containers share network + volumes; labels/annotations;
Downward API; restart policy; init containers; sidecars; pod phases/conditions; requests/limits basics;
why you rarely create bare pods in production.

Run from this folder (`cd 01-pods`) with the cluster from module 00.

## 1. Basic pod
```bash
kubectl apply -f pod-basic.yaml
kubectl get pod hello-pod -o wide -w          # Ctrl+C when Running (Pending -> ContainerCreating -> Running)
kubectl describe pod hello-pod                # note Events, QoS Class, Node, IP
kubectl logs hello-pod
kubectl port-forward pod/hello-pod 8080:8080 &
curl localhost:8080/hello                     # pod, node, namespace come from the Downward API
curl localhost:8080/actuator/health
kill %1                                       # stop port-forward
kubectl exec -it hello-pod -- sh              # try: id, env | sort, ls /app
```

## 2. A bare pod is NOT self-healing
```bash
kubectl delete pod hello-pod                  # gone forever (nothing recreates it) -> use Deployments (module 03)
```

## 3. Multi-container pod (sidecar pattern)
```bash
kubectl apply -f pod-sidecar.yaml
kubectl get pod sidecar-pod                   # READY 3/3
kubectl exec sidecar-pod -c web -- cat /usr/share/nginx/html/index.html   # written by the OTHER container
kubectl logs sidecar-pod -c localhost-checker # proves shared network namespace (localhost)
```

## 4. Init containers
```bash
kubectl apply -f pod-init.yaml
kubectl get pod init-pod -w                   # STATUS: Init:0/2 -> Init:1/2 -> PodInitializing -> Running
kubectl logs init-pod -c wait-for-dns
kubectl exec init-pod -- cat /usr/share/nginx/html/index.html
```

## 5. Debugging a failing pod
```bash
kubectl apply -f pod-crashloop.yaml
kubectl get pod crashy -w                     # Error -> CrashLoopBackOff (exponential back-off restarts)
kubectl logs crashy --previous                # logs of the last crashed container
kubectl describe pod crashy | grep -A8 "Last State"   # Exit Code 1
```

## 6. Pod lifecycle facts
- Phases: `Pending -> Running -> Succeeded | Failed` (+ `Unknown`).
- Container states: Waiting / Running / Terminated. `restartPolicy` applies to all containers in the pod.
- `kubectl delete pod` sends SIGTERM, waits `terminationGracePeriodSeconds` (default 30s), then SIGKILL.
- Pod IPs are ephemeral; never address a pod directly - use a Service (module 04).

## 7. Generate YAML instead of typing it
```bash
kubectl run demo --image=nginx:1.27-alpine --port=80 --dry-run=client -o yaml
```

## Cleanup
```bash
kubectl delete -f . --ignore-not-found
```
