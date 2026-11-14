# Module 04 - Services & service discovery

**Concepts:** Service types (ClusterIP, NodePort, LoadBalancer, Headless, ExternalName), selectors,
Endpoints/EndpointSlices, kube-dns (CoreDNS), environment variables, session affinity, port-forward.

```bash
cd 04-services
kubectl create namespace demo 2>/dev/null
kubectl apply -f app.yaml
kubectl apply -f services.yaml
kubectl -n demo get svc,endpoints -o wide        # selector -> endpoints (pod IPs)
```

## 1. ClusterIP + DNS (from inside the cluster)
```bash
kubectl -n demo run curl --rm -it --image=curlimages/curl --restart=Never -- sh
# inside the pod:
curl hello-clusterip/hello                                   # short name (same namespace)
curl hello-clusterip.demo/hello                              # <svc>.<namespace>
curl hello-clusterip.demo.svc.cluster.local/hello            # FQDN
for i in 1 2 3 4 5 6; do curl -s hello-clusterip/hello | grep -o '"pod":"[^"]*"'; done   # load balanced
nslookup hello-clusterip                                     # -> the ClusterIP
nslookup hello-headless                                      # -> ALL pod IPs (headless)
nslookup external-api                                        # -> CNAME example.com
exit
```

## 2. NodePort (from your laptop)
```bash
curl localhost:30080/hello           # kind config maps host 30080 -> node 30080
```

## 3. Endpoints follow readiness
```bash
kubectl -n demo get endpointslices
kubectl -n demo scale deployment hello --replicas=1
kubectl -n demo get endpoints hello-clusterip          # 1 IP
kubectl -n demo scale deployment hello --replicas=3
```
A pod that fails its readiness probe is removed from endpoints automatically (module 09).

## 4. Environment-variable discovery (legacy)
```bash
kubectl -n demo exec deploy/hello -- env | grep HELLO_CLUSTERIP     # only for services created BEFORE the pod
```

## 5. LoadBalancer
```bash
kubectl -n demo get svc hello-lb            # EXTERNAL-IP <pending> on plain kind
# optional: run `cloud-provider-kind` (separate binary) in another terminal -> gets a local IP
```

## 6. Session affinity
```bash
kubectl -n demo patch svc hello-clusterip -p '{"spec":{"sessionAffinity":"ClientIP"}}'
# repeat the curl loop: the same pod answers every time. Revert with "None".
```

## 7. port-forward (debugging, not for production)
```bash
kubectl -n demo port-forward svc/hello-clusterip 8081:80
```

## How it works (architect notes)
- `kube-proxy` programs iptables/IPVS rules on every node: ClusterIP -> one of the endpoint pod IPs.
- ClusterIP is virtual: it is not pingable, only the service ports work.
- Internal traffic: Service. North-south HTTP traffic: Ingress / Gateway API (module 08).

## Cleanup
```bash
kubectl delete namespace demo
```
