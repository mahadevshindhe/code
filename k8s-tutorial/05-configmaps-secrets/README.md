# Module 05 - ConfigMaps & Secrets

**Concepts:** externalised config (12-factor), env vars vs mounted files, update propagation,
immutable ConfigMaps, Secret types, base64 != encryption, creating from files/literals, restarting pods on change.

```bash
cd 05-configmaps-secrets
kubectl create namespace demo 2>/dev/null
kubectl apply -f config.yaml
kubectl apply -f deployment.yaml
kubectl -n demo rollout status deploy/hello
kubectl -n demo port-forward deploy/hello 8080:8080 &
```

## 1. Verify each injection style
```bash
curl -s localhost:8080/env                                              # APP_GREETING, FEATURE_FLAG(APP_ENV) from ConfigMap
curl -s "localhost:8080/file?path=/etc/app-config/app.properties"       # mounted ConfigMap file
curl -s "localhost:8080/secret?path=/etc/secret/db_password"            # secret file exists, length only
kubectl -n demo exec deploy/hello -- ls -l /etc/app-config /etc/secret
kubectl -n demo exec deploy/hello -- env | grep API_TOKEN               # secret as env var (visible to anything in the container!)
```

## 2. Secrets are only base64
```bash
kubectl -n demo get secret hello-secret -o yaml
kubectl -n demo get secret hello-secret -o jsonpath='{.data.db_password}' | base64 -d ; echo
```
Anyone with `get secret` RBAC can read it. Mitigations: RBAC (module 14), etcd encryption at rest, external
secret managers (External Secrets Operator, Vault, cloud KMS), Sealed Secrets for GitOps.

## 3. Update propagation
```bash
kubectl -n demo patch configmap hello-config --type merge -p '{"data":{"app.properties":"greeting=Changed\nretries=9\n"}}'
sleep 70
curl -s "localhost:8080/file?path=/etc/app-config/app.properties"       # file updated WITHOUT restart
curl -s localhost:8080/env | grep APP_GREETING                           # env vars are NOT updated until pod restarts
kubectl -n demo rollout restart deployment/hello                         # pick up env changes
```
Pattern for automatic rollouts: include a config hash annotation in the pod template (Helm: `checksum/config`)
or use Kustomize `configMapGenerator` (module 19).

## 4. Create from the CLI
```bash
echo "key=value" > /tmp/demo.properties
kubectl -n demo create configmap from-file --from-file=/tmp/demo.properties
kubectl -n demo create configmap from-literal --from-literal=A=1 --from-literal=B=2
kubectl -n demo create secret generic db --from-literal=password=abc123
kubectl -n demo create secret docker-registry regcred --docker-server=ghcr.io --docker-username=u --docker-password=p
kubectl -n demo create secret tls demo-tls --cert=tls.crt --key=tls.key       # see module 08 for generating certs
```

## 5. Immutable ConfigMaps (safer + less API-server load)
```yaml
immutable: true      # add at top level of ConfigMap/Secret; change requires creating a new object
```

## 6. Spring Boot tip
Mount a Secret directory at `/run/secrets` and use `spring.config.import=optional:configtree:/run/secrets/`
(done in `apps/notes-app`, module 17).  Spring Cloud Kubernetes can also watch ConfigMaps directly.

## Cleanup
```bash
kill %1; kubectl delete namespace demo
```
