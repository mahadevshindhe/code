# Module 07 - Resource limits & JVM in containers

**Concepts:** cgroups, `--memory`, `--cpus`, `--pids-limit`, OOMKilled, `docker stats`,
JVM container support (`MaxRAMPercentage`, `availableProcessors`), heap vs container memory.

## Run
```bash
cd 07-resource-limits
docker compose up -d --build
docker compose ps
```

## 1. See that the JVM respects container limits
```bash
curl -s localhost:8081/hello      # availableProcessors=1 , maxHeapMB ~ 128  (limited: 256MB * 50%)
curl -s localhost:8082/hello      # all host CPUs, heap = 25% of host RAM (default)
```

## 2. Watch usage live (open a second terminal)
```bash
docker stats
```

## 3. CPU limit
```bash
curl "localhost:8081/burn?seconds=15"    # in docker stats: CPU% stays near 50%
curl "localhost:8082/burn?seconds=15"    # uses all cores
```

## 4. Java heap OOM (JVM catches it first) - container restarts due to ExitOnOutOfMemoryError
```bash
curl "localhost:8081/alloc?mb=50"        # fine
curl "localhost:8081/alloc?mb=200"       # OutOfMemoryError -> JVM exits (exit code 3)
docker compose ps -a
docker inspect limits-demo-app-limited-1 --format '{{.State.ExitCode}} OOMKilled={{.State.OOMKilled}}'
```

## 5. Container OOM kill (kernel kills the process, OOMKilled=true)
Set `-XX:MaxRAMPercentage=95` and `mem_limit: 128m`, restart, call `/alloc?mb=300` and check:
```bash
docker inspect <container> --format 'OOMKilled={{.State.OOMKilled}} exit={{.State.ExitCode}}'   # 137 if kernel killed it
```

## 6. Update limits on a running container
```bash
docker update --memory 512m --memory-swap 512m --cpus 1 limits-demo-app-limited-1
```

## Plain docker run equivalents
```bash
docker run -d --memory=256m --memory-swap=256m --cpus=0.5 --pids-limit=200 -p 8083:8080 hello-app:layered
```

## Architect's guidance
- Always set a memory limit; always size the JVM relative to it (`MaxRAMPercentage` 50-75%).
- Heap is only part of JVM memory (metaspace, threads, direct buffers) - leave headroom.
- CPU limit < 1 core makes JVM see 1 CPU -> GC/ForkJoin pools are small. Consider `-XX:ActiveProcessorCount`.
- Kubernetes: these map to `resources.requests/limits`.

## Cleanup
```bash
docker compose down
docker rm -f $(docker ps -aq --filter ancestor=hello-app:layered) 2>/dev/null
```
