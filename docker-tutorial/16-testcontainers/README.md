# Module 16 - Testcontainers: Docker in your Java integration tests

**Concepts:** ephemeral real dependencies for tests, `@ServiceConnection`, no mocks for DB/Redis,
parallel-safe random ports, CI parity with production.

Test source: `apps/notes-app/src/test/java/com/example/notes/NotesIntegrationTest.java`

## Run (needs JDK 21 + Maven + running Docker daemon)
```bash
mvn -f apps/notes-app/pom.xml test
```
While the tests run, in another terminal:
```bash
docker ps        # you will see postgres:16-alpine, redis:7-alpine and a testcontainers/ryuk reaper container
```
They are started before the tests and removed afterwards automatically (Ryuk guarantees cleanup, even if the JVM crashes).

## How it works
- `@Testcontainers` + `@Container` start the containers.
- `@ServiceConnection` (Spring Boot 3.1+) auto-wires `spring.datasource.*` / `spring.data.redis.*`
  to the random mapped ports - no `@DynamicPropertySource` boilerplate.
- Containers are `static` -> shared by all tests in the class (fast). Use singleton pattern across classes.

## Tips
- Docker Desktop / Colima / Podman: set `DOCKER_HOST` if the socket is non-standard.
- CI: run on a runner with Docker available (GitHub Actions ubuntu runners have it).
- Faster: enable container reuse with `~/.testcontainers.properties`: `testcontainers.reuse.enable=true` and `.withReuse(true)`.
