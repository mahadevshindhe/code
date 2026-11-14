# Module 15 - Buildx: multi-architecture images (amd64 + arm64)

**Concepts:** BuildKit/buildx, builders, QEMU emulation, manifest lists, build cache export,
Apple Silicon (arm64) vs cloud (amd64), build args/secrets.

Relevant if developers use Apple Silicon and production runs on amd64 (or Graviton/arm64).

## 1. Create a builder
```bash
docker buildx ls
docker buildx create --name multi --driver docker-container --use
docker buildx inspect --bootstrap
```

## 2. Build for two platforms and push to the local registry (module 10 must be running)
```bash
cd 10-local-registry && docker compose up -d && cd ..
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -f 02-multistage-builds/Dockerfile.layered \
  -t localhost:5000/demo/hello-app:multiarch \
  --push apps/hello-app
docker buildx imagetools inspect localhost:5000/demo/hello-app:multiarch      # shows both platforms
```
> If the builder cannot reach `localhost:5000` (separate container), use `--driver-opt network=host` when creating it.

## 3. Run a specific platform
```bash
docker run --rm --platform linux/amd64 alpine uname -m     # x86_64 (emulated on Apple Silicon)
docker run --rm --platform linux/arm64 alpine uname -m     # aarch64
```

## 4. Build cache for CI
```bash
docker buildx build --cache-to type=local,dest=/tmp/bx-cache --cache-from type=local,src=/tmp/bx-cache \
  -f 02-multistage-builds/Dockerfile.layered -t hello-app:ci --load apps/hello-app
```

## 5. Build-time secrets (never stored in layers)
```dockerfile
# in Dockerfile:  RUN --mount=type=secret,id=nexus_token  TOKEN=$(cat /run/secrets/nexus_token) mvn deploy
```
```bash
docker buildx build --secret id=nexus_token,src=./token.txt -t app .
```

## Cleanup
```bash
docker buildx rm multi
```
