# Module 13 - Security hardening checklist

**Concepts:** non-root, read-only rootfs, capabilities, no-new-privileges, tmpfs, seccomp/AppArmor,
vulnerability scanning, SBOM, secrets, minimal base images, pinned digests, network exposure.

## Run the hardened container
```bash
cd 13-security-hardening
docker compose up -d
curl localhost:8080/hello
```

## Prove the hardening
```bash
docker compose exec app id                                   # uid=<non-zero> app (not root)
docker compose exec app sh -c 'touch /app/x'                 # fails: read-only file system
docker compose exec app sh -c 'touch /tmp/x && echo "tmp ok"'
docker inspect hardened-app-1 --format 'CapDrop={{.HostConfig.CapDrop}} RO={{.HostConfig.ReadonlyRootfs}} {{.HostConfig.SecurityOpt}}'
docker run --rm --cap-drop ALL alpine chown nobody /tmp 2>&1 | head -1     # operation not permitted
```
Compare with a default container (`docker run --rm alpine id` -> root, many capabilities).

## Scan images for CVEs
```bash
docker scout quickview hello-app:layered           # Docker Scout (included in Docker Desktop)
docker scout cves hello-app:layered
# or open source tools:
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock aquasec/trivy image hello-app:layered
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock anchore/syft hello-app:layered   # SBOM
```

## Lint the Dockerfile
```bash
docker run --rm -i hadolint/hadolint < ../02-multistage-builds/Dockerfile.layered
```

## Checklist
- [ ] Non-root `USER`; `read_only: true` + tmpfs for temp dirs
- [ ] `cap_drop: ALL`, add back only what is needed; `no-new-privileges`
- [ ] Minimal base (alpine / distroless / chainguard); multi-stage so build tools never ship
- [ ] Pin base images by tag + digest; rebuild regularly; scan in CI
- [ ] No secrets in `ENV`, `ARG`, image layers or git (`docker history` reveals ARGs!)
- [ ] Never mount `/var/run/docker.sock` into app containers (= root on host)
- [ ] Never use `--privileged`; avoid `--network host`
- [ ] Publish only needed ports, bind to `127.0.0.1` where possible
- [ ] Set memory/CPU/pids limits
- [ ] Sign images (cosign / Docker Content Trust: `export DOCKER_CONTENT_TRUST=1`)
