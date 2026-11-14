# Module 10 - Registries: tag, push, pull

**Concepts:** image naming (`registry/namespace/repo:tag`), tags vs digests, `docker login`,
push/pull, private registry, semantic version tagging strategy, retention.

## Run a local registry
```bash
cd 10-local-registry
docker compose up -d
```
`localhost:5000` is treated as an insecure-allowed registry by Docker automatically.

## Push your Java app image
```bash
docker build -f ../02-multistage-builds/Dockerfile.layered -t hello-app:layered ../apps/hello-app   # if not built yet
docker tag hello-app:layered localhost:5000/demo/hello-app:1.0.0
docker tag hello-app:layered localhost:5000/demo/hello-app:latest
docker push localhost:5000/demo/hello-app:1.0.0
docker push localhost:5000/demo/hello-app:latest
```

## Inspect the registry
```bash
curl localhost:5000/v2/_catalog
curl localhost:5000/v2/demo/hello-app/tags/list
# UI: http://localhost:5001
```

## Pull from "somewhere else"
```bash
docker rmi localhost:5000/demo/hello-app:1.0.0 localhost:5000/demo/hello-app:latest
docker pull localhost:5000/demo/hello-app:1.0.0
docker run --rm -p 8080:8080 localhost:5000/demo/hello-app:1.0.0
```

## Digests (immutable reference)
```bash
docker images --digests localhost:5000/demo/hello-app
# pin in production:  image: localhost:5000/demo/hello-app@sha256:<digest>
```

## Using real registries
```bash
docker login                                   # Docker Hub
docker login ghcr.io -u <user>                 # GitHub Container Registry (PAT as password)
aws ecr get-login-password | docker login --username AWS --password-stdin <acct>.dkr.ecr.<region>.amazonaws.com
docker tag hello-app:layered ghcr.io/<org>/hello-app:1.0.0 && docker push ghcr.io/<org>/hello-app:1.0.0
```

## Tagging strategy (architect notes)
- Immutable: `1.4.2`, git SHA `a1b2c3d`. Moving: `1.4`, `latest`. Deploy immutable tags/digests only.
- Add OCI labels: `org.opencontainers.image.source`, `.revision`, `.version`.

## Cleanup
```bash
docker compose down -v
```
