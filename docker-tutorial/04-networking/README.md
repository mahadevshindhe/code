# Module 04 - Docker Networking

**Concepts:** bridge (default) vs user-defined bridge, DNS service discovery, port publishing,
host network, none, network aliases, multiple networks, internal networks, connect/disconnect.

Prereq: `hello-app:layered` (module 02) - or replace with `hello-app:basic`.

## 1. List networks
```bash
docker network ls          # bridge, host, none exist by default
docker network inspect bridge
```

## 2. Default bridge: NO name resolution
```bash
docker run -d --name a1 hello-app:layered
docker run -d --name a2 hello-app:layered
docker exec a2 sh -c "wget -qO- http://a1:8080/hello" || echo "FAILED - no DNS on default bridge"
docker inspect -f '{{.NetworkSettings.IPAddress}}' a1      # works with IP only
docker rm -f a1 a2
```

## 3. User-defined bridge: automatic DNS
```bash
docker network create --driver bridge --subnet 172.30.0.0/24 mynet
docker run -d --name b1 --network mynet hello-app:layered
docker run -d --name b2 --network mynet hello-app:layered
docker exec b2 wget -qO- http://b1:8080/hello               # works - container name is DNS name
docker network inspect mynet
```

## 4. Network aliases (same alias on several containers = DNS round-robin)
```bash
docker run -d --name b3 --network mynet --network-alias backend hello-app:layered
docker run -d --name b4 --network mynet --network-alias backend hello-app:layered
docker exec b1 sh -c 'for i in 1 2 3 4 5 6; do wget -qO- http://backend:8080/hello; echo; done'
# hostname alternates between b3/b4 container IDs
```

## 5. Port publishing vs. container-to-container traffic
```bash
docker run -d --name pub --network mynet -p 9090:8080 hello-app:layered
curl localhost:9090/hello                    # host -> container needs -p
docker exec b1 wget -qO- http://pub:8080/hello   # container -> container uses container port, no -p needed
docker run -d --name loc -p 127.0.0.1:9091:8080 hello-app:layered   # bind to localhost only
```

## 6. Multiple networks / isolation (like frontend + backend tiers)
```bash
docker network create front
docker network create --internal back          # --internal = no outbound internet
docker run -d --name web --network front hello-app:layered
docker network connect back web                # attach 2nd network to running container
docker run -d --name db --network back hello-app:layered
docker exec web wget -qO- http://db:8080/hello       # works: web is on 'back'
docker exec db wget -T 3 -qO- http://example.com || echo "no internet: back is internal"
docker network disconnect back web
```

## 7. host and none
```bash
docker run --rm --network none alpine ip addr       # only loopback
docker run --rm --network host alpine ip addr       # host's interfaces (Linux only; limited on Docker Desktop)
```

## 8. Reach the host from a container
`host.docker.internal` works on Docker Desktop. On Linux add:
`--add-host=host.docker.internal:host-gateway`

## Cleanup
```bash
docker rm -f b1 b2 b3 b4 pub loc web db 2>/dev/null
docker network rm mynet front back
docker network prune
```
