# Module 11 - Build images WITHOUT a Dockerfile (Jib / Spring Boot buildpacks)

**Concepts:** daemonless builds, reproducible layering for Java, buildpacks, CI-friendly image builds.

The Jib plugin is already configured in `apps/hello-app/pom.xml`.

## Option A: Jib -> local Docker daemon
Needs JDK 21 + Maven on the host. From the **repository root**:
```bash
mvn -f apps/hello-app/pom.xml compile jib:dockerBuild
docker images hello-app            # hello-app:jib
docker run --rm -p 8080:8080 hello-app:jib
curl localhost:8080/hello          # "Hello from Jib"
```
Jib builds separate layers for dependencies, resources and classes - no Docker daemon required in CI:
```bash
# push straight to a registry (no docker needed)
mvn -f apps/hello-app/pom.xml compile jib:build -Dimage=localhost:5000/demo/hello-app:jib -Djib.allowInsecureRegistries=true
# build a tarball
mvn -f apps/hello-app/pom.xml compile jib:buildTar
```

## Option B: Spring Boot Cloud Native Buildpacks
```bash
mvn -f apps/hello-app/pom.xml spring-boot:build-image -Dspring-boot.build-image.imageName=hello-app:buildpack
docker run --rm -p 8080:8080 hello-app:buildpack
```
Buildpacks add a memory calculator, SBOM, and a non-root user automatically.

## Dockerfile vs Jib vs Buildpacks
| | Dockerfile | Jib | Buildpacks |
|--|--|--|--|
| Control | Full | Medium | Low |
| Needs Docker daemon | yes | no | yes |
| Java-specific optimisation | manual | automatic | automatic |
| Non-Java stack | yes | no | yes |
