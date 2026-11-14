# Module 05 - Docker Compose: multi-container application

**Concepts:** compose file anatomy, services, build vs image, named volumes, networks (tiers),
healthchecks, `depends_on: service_healthy`, `.env` substitution, profiles, restart policy,
scaling, logs, exec, down vs down -v.

Architecture:
```
 host:8080 -> [app (Spring Boot)] --backend--> [postgres]  (volume pgdata)
                    |                    \--> [redis]     (volume redisdata)
 frontend network        backend network (internal)
```

## Run (from this folder)
```bash
cd 05-compose-full-stack
docker compose up -d --build         # builds notes-app image (first time ~2-3 min) and starts everything
docker compose ps                    # wait for (healthy)
docker compose logs -f app
```

## Try it
```bash
curl -X POST localhost:8080/notes -H 'Content-Type: application/json' -d '{"text":"first note"}'
curl -X POST localhost:8080/notes -H 'Content-Type: application/json' -d '{"text":"second note"}'
curl localhost:8080/notes
curl localhost:8080/visits
curl localhost:8080/actuator/health
```

## Prove persistence
```bash
docker compose down                  # removes containers + networks, KEEPS volumes
docker compose up -d
curl localhost:8080/notes            # notes are still there
docker compose down -v               # -v also deletes volumes (data lost)
```

## Useful commands
```bash
docker compose exec postgres psql -U notes -d notes -c 'select * from notes;'
docker compose exec redis redis-cli get visits
docker compose exec app sh
docker compose restart app
docker compose stop / start
docker compose config                # validate + show final merged file with env substituted
docker compose top
docker compose images
```

## Profiles (optional DB UI)
```bash
docker compose --profile tools up -d
# open http://localhost:8090  -> System: PostgreSQL, Server: postgres, User/DB: notes, Password: notes
```

## Change config without editing files
```bash
DB_PASSWORD=s3cr3t APP_PORT=9000 docker compose up -d      # note: changing the DB password after DB init
                                                           # requires `down -v` since PG stores it in the volume
```

## Scale (needs no fixed host port; shown in module 08)
```bash
docker compose up -d --scale app=3     # will fail on port 8080 conflict -> see module 08 for the proper way
```

## Things to observe
- App starts only after postgres/redis are healthy (`depends_on.condition`).
- `postgres` and `redis` are **not** reachable from the host - there are no `ports:` for them, and the
  backend network is `internal`.
- `docker network ls` shows `notes-stack_frontend` and `notes-stack_backend` (project-name prefix).
