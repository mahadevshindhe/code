# Module 02 - Multi-stage builds, layer caching, image size

**Concepts:** multi-stage, build cache, BuildKit cache mounts, layered jars, `.dockerignore`,
distroless, non-root, HEALTHCHECK, image size comparison.

Run from the **repository root**. No local Maven/JDK needed - the build happens inside Docker.

```bash
export DOCKER_BUILDKIT=1     # default in modern Docker; needed for --mount=type=cache

docker build -f 02-multistage-builds/Dockerfile.multistage -t hello-app:multistage apps/hello-app
docker build -f 02-multistage-builds/Dockerfile.layered    -t hello-app:layered    apps/hello-app
docker build -f 02-multistage-builds/Dockerfile.distroless -t hello-app:distroless apps/hello-app

docker images hello-app            # compare SIZE column
```

## Experiments
1. **Layer cache:** rebuild with no change - every step says `CACHED`.
   Now edit `HelloController.java` (change a string) and rebuild `:layered`.
   Only the last layers rebuild; dependencies layer stays cached.
2. **See layers:** `docker history hello-app:layered` - note the dependency layer is much bigger than the app layer.
3. **Healthcheck:**
   ```bash
   docker run -d --name h1 -p 8080:8080 hello-app:layered
   sleep 40 && docker ps          # STATUS shows (healthy)
   docker inspect --format '{{json .State.Health}}' h1
   docker rm -f h1
   ```
4. **Distroless:** `docker run -d --name h2 -p 8080:8080 hello-app:distroless`
   then `docker exec -it h2 sh` fails (no shell) - this is the security benefit.
   Use `docker run --rm -it --entrypoint sh gcr.io/distroless/java21-debian12:debug` for a debug variant.
5. **Build a specific stage:** `docker build --target build -f 02-multistage-builds/Dockerfile.multistage -t hello-app:buildstage apps/hello-app`
6. **No cache:** `docker build --no-cache ...`
7. **Plain progress output:** `docker build --progress=plain ...`

## Rules of thumb
- Put things that change **least** first (pom -> deps -> source).
- Use `.dockerignore` to keep the build context small (`apps/hello-app/.dockerignore`).
- Prefer JRE (or jlink/distroless) over JDK at runtime.
- Never run as root; always pin base image tags (even better: digests).
