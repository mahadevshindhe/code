# Module 03 - Volumes, bind mounts, tmpfs

**Concepts:** container filesystem is ephemeral; named volumes; bind mounts; tmpfs; read-only mounts;
backup/restore; volume drivers.

Prereq: build the image used in this module (a multi-stage image with a writable `/data` dir
for the non-root user - see `Dockerfile.with-data`). Run from the **repository root**:

```bash
docker build -f 03-volumes-and-storage/Dockerfile.with-data -t hello-app:data apps/hello-app
```
The app endpoint `/visit` increments a counter stored in `/data/visits.txt`.

## 1. Without a volume -> data is lost with the container
```bash
docker run -d --name v0 -p 8080:8080 hello-app:data
sleep 15
curl localhost:8080/visit ; curl localhost:8080/visit        # visits: 1, 2
docker rm -f v0
docker run -d --name v0 -p 8080:8080 hello-app:data
sleep 15
curl localhost:8080/visit                                    # visits: 1  (data gone!)
docker rm -f v0
```

## 2. Named volume (managed by Docker - preferred for databases)
```bash
docker volume create demo-data
docker run -d --name v1 -p 8080:8080 -v demo-data:/data hello-app:data
curl localhost:8080/visit ; curl localhost:8080/visit
docker rm -f v1                                              # container gone...
docker run -d --name v1 -p 8080:8080 -v demo-data:/data hello-app:data
curl localhost:8080/visit                                    # visits: 3  (volume survived)
docker volume ls
docker volume inspect demo-data                              # Mountpoint, driver
```

## 3. Bind mount (host folder -> container; great for local dev)
```bash
mkdir -p ./host-data
docker run -d --name v2 -p 8081:8080 -v "$(pwd)/host-data:/data" hello-app:data
curl localhost:8081/visit
cat ./host-data/visits.txt                                    # file visible on host!
```
Windows PowerShell: `-v "${PWD}/host-data:/data"`.
Linux hosts may hit permission issues: add `--user $(id -u):$(id -g)`.

## 4. Read-only mount
```bash
docker run -d --name v3 -p 8082:8080 -v "$(pwd)/host-data:/data:ro" hello-app:data
curl localhost:8082/visit        # fails with 500 - read-only file system
```

## 5. tmpfs (in-memory, never hits disk - good for secrets/temp)
```bash
docker run -d --name v4 --tmpfs /data:rw,size=16m,uid=100 -p 8083:8080 hello-app:data
```

## 6. Backup & restore a volume
```bash
# backup
docker run --rm -v demo-data:/data -v "$(pwd)":/backup alpine tar czf /backup/demo-data.tgz -C /data .
# restore into a new volume
docker volume create demo-data-restored
docker run --rm -v demo-data-restored:/data -v "$(pwd)":/backup alpine tar xzf /backup/demo-data.tgz -C /data
docker run --rm -v demo-data-restored:/data alpine cat /data/visits.txt
```

## 7. Share a volume between containers
```bash
docker run --rm -v demo-data:/data alpine sh -c 'echo shared > /data/hello.txt'
docker run --rm -v demo-data:/data alpine ls -l /data
```

## Cleanup
```bash
docker rm -f v1 v2 v3 v4 2>/dev/null
docker volume rm demo-data demo-data-restored
docker volume prune          # removes ALL unused volumes - careful!
rm -rf host-data demo-data.tgz
```

| Type         | Managed by | Typical use                          |
|--------------|-----------|---------------------------------------|
| Named volume | Docker    | DB data, anything that must persist   |
| Bind mount   | You       | Source code in dev, config files      |
| tmpfs        | Memory    | Temp/sensitive data                   |
