#!/usr/bin/env bash
# Stops and removes everything created by the tutorial modules.
cd "$(dirname "$0")/.."
for d in 05-compose-full-stack 06-config-and-secrets 07-resource-limits 08-load-balancing-nginx \
         09-observability 10-local-registry 13-security-hardening 14-dev-workflow-debug; do
  (cd "$d" && docker compose down -v --remove-orphans 2>/dev/null)
done
docker stack rm demo 2>/dev/null
docker rm -f $(docker ps -aq --filter "ancestor=hello-app:layered") 2>/dev/null
docker rmi hello-app:basic hello-app:multistage hello-app:layered hello-app:distroless hello-app:data \
           hello-app:jib hello-app:buildpack notes-app:1.0 echo-demo 2>/dev/null
echo "Cleanup done. (Run 'docker system prune' for dangling data, 'docker swarm leave --force' if you used module 12)"
