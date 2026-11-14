# Module 08 - Ingress (HTTP routing, TLS)

**Concepts:** Ingress vs Service, Ingress controller (ingress-nginx), IngressClass, host/path routing,
TLS termination, annotations, canary at ingress level; Gateway API as successor.

Prereq: cluster from module 00 (ingress-nginx installed; host ports 8080/8443 mapped).

```bash
cd 08-ingress
kubectl create namespace demo 2>/dev/null
kubectl apply -f apps.yaml
kubectl -n ingress-nginx get pods,svc            # the controller
kubectl get ingressclass
```

## 1. Host-based routing
```bash
kubectl apply -f ingress-host.yaml
kubectl -n demo get ingress
curl http://hello.localtest.me:8080/hello         # -> hello-app
curl http://web.localtest.me:8080/                # -> nginx welcome page
# without relying on DNS:
curl -H "Host: hello.localtest.me" http://localhost:8080/hello
```

## 2. Path-based routing
```bash
kubectl apply -f ingress-path.yaml
curl http://apps.localtest.me:8080/hello          # -> hello-app
curl http://apps.localtest.me:8080/               # -> nginx
kubectl -n demo describe ingress path-routing     # see backends + events
```

## 3. TLS
```bash
./create-tls.sh
kubectl apply -f ingress-tls.yaml
curl -k https://secure.localtest.me:8443/hello            # -k: self-signed cert
curl -vk https://secure.localtest.me:8443/hello 2>&1 | grep -E "subject|issuer"
curl -i http://secure.localtest.me:8080/hello             # 308 redirect to https
```
Production: install **cert-manager** + ClusterIssuer (Let's Encrypt) to issue/renew certs automatically.

## 4. Useful annotations (ingress-nginx)
`nginx.ingress.kubernetes.io/` -> `rewrite-target`, `proxy-body-size`, `ssl-redirect`, `whitelist-source-range`,
`limit-rps`, `affinity: cookie`, `canary: "true"` + `canary-weight: "20"`, `auth-type: basic`.

## 5. Debugging
```bash
kubectl -n ingress-nginx logs -l app.kubernetes.io/component=controller --tail=20
kubectl -n demo get endpoints hello web           # empty endpoints => 503 from ingress
```

## Architect's notes
- Ingress handles L7 HTTP(S) only. TCP/UDP or richer policy: **Gateway API** (`Gateway`, `HTTPRoute`) - the successor.
- One Ingress controller = one cloud LB for many services (saves cost).
- Service mesh (Istio/Linkerd) adds mTLS, retries, traffic splitting between services.

## Cleanup
```bash
kubectl delete namespace demo
```
