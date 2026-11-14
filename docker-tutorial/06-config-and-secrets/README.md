# Module 06 - Config & Secrets

**Concepts:** `environment`, `env_file`, `.env` substitution, compose `configs`, compose `secrets`,
`*_FILE` convention, why not to bake secrets into images, Spring `configtree`.

| Mechanism        | Where it ends up in container        | Use for                         |
|------------------|--------------------------------------|---------------------------------|
| `environment`    | env var                              | non-sensitive flags             |
| `env_file`       | env vars                             | many non-sensitive values       |
| `configs`        | file (default `/<name>`)             | app config files                |
| `secrets`        | file `/run/secrets/<name>` (tmpfs)   | passwords, tokens, keys         |

## Run
```bash
cd 06-config-and-secrets
docker compose up -d --build
docker compose ps
```

## Verify each mechanism
```bash
curl localhost:8080/info          # banner comes from mounted config file; dbUrl from env_file
curl localhost:8080/env           # APP_ENV=local from inline environment
curl "localhost:8080/notes"       # works => app connected to Postgres using password from the SECRET FILE
curl "localhost:8080/secret?name=db_password"   # {"secretFileExists":true,"length":14}  (never exposes value)

docker compose exec app ls -l /run/secrets /app/config
docker compose exec app env | grep -i password || echo "password is NOT in env vars"
docker inspect config-demo-app-1 --format '{{json .Config.Env}}'   # secret not visible here either
```

## How the Java app reads the secret
`apps/notes-app/src/main/resources/application.properties`:
```properties
spring.config.import=optional:configtree:/run/secrets/
spring.datasource.password=${db_password:${DB_PASSWORD:notes}}
```
Order of precedence: secret file -> `DB_PASSWORD` env -> default.

## Experiment: change the banner without rebuilding
Edit `app-config.properties`, then `docker compose up -d --force-recreate app`, `curl localhost:8080/info`.

## Cleanup
```bash
docker compose down -v
```

> Demo password file is committed only for the tutorial. In real projects add `secrets/` to `.gitignore`
> and use Docker Swarm secrets, Kubernetes secrets, Vault, or cloud secret managers.
