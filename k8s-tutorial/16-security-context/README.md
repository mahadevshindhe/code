# Module 16 - Pod security: securityContext & Pod Security Admission

**Concepts:** Pod Security Standards (privileged/baseline/restricted), Pod Security Admission labels,
`securityContext` (runAsNonRoot, readOnlyRootFilesystem, capabilities, seccomp, privilege escalation),
service-account token hygiene, policy engines (Kyverno / OPA Gatekeeper / ValidatingAdmissionPolicy).

```bash
cd 16-security-context
kubectl apply -f psa-namespace.yaml
```

## 1. The cluster rejects insecure pods
```bash
kubectl apply -f pod-insecure.yaml
# Error ... violates PodSecurity "restricted:latest": privileged, allowPrivilegeEscalation != false,
#           unrestricted capabilities, runAsNonRoot != true, seccompProfile ...
```

## 2. Hardened Java deployment is admitted
```bash
kubectl apply -f pod-hardened.yaml
kubectl -n psa-demo rollout status deploy/hardened-hello
kubectl -n psa-demo exec deploy/hardened-hello -- id                         # uid=1000
kubectl -n psa-demo exec deploy/hardened-hello -- sh -c 'touch /app/x'        # Read-only file system
kubectl -n psa-demo exec deploy/hardened-hello -- sh -c 'touch /tmp/x && echo tmp ok'
kubectl -n psa-demo exec deploy/hardened-hello -- cat /proc/1/status | grep Cap   # CapEff all zeros
```

## 3. Try breaking rules one by one (edit pod-hardened.yaml, apply, read the rejection)
- remove `runAsNonRoot` / `runAsUser`
- remove `capabilities.drop`
- set `allowPrivilegeEscalation: true`
- add `hostNetwork: true`  (baseline violation)

## 4. Modes
| Label suffix | Effect |
|---|---|
| `enforce` | reject pod |
| `audit` | allow, record in audit log |
| `warn` | allow, warn the kubectl user |
Roll out policies gradually: `warn` + `audit` first, then `enforce`.

## Supply-chain & runtime hardening checklist
- Minimal images (distroless/alpine), scan in CI (Trivy/Grype), sign (cosign), admission verify (Kyverno/Sigstore policy-controller)
- Pin images by digest; private registry + `imagePullSecrets`
- No `hostPath`, no `hostNetwork`, no `docker.sock` mounts, never `privileged`
- Secrets: external secret manager, etcd encryption at rest, short-lived tokens
- NetworkPolicies (module 15), RBAC least privilege (module 14), audit logging
- Runtime detection: Falco / Tetragon

## Cleanup
```bash
kubectl delete namespace psa-demo
```
