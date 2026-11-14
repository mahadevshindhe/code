#!/usr/bin/env bash
# Builds the sample images with Docker and loads them into the kind nodes
# (kind nodes cannot see your local Docker images otherwise!).
set -euo pipefail
cd "$(dirname "$0")/.."
docker build -t hello-app:1.0 apps/hello-app
docker tag hello-app:1.0 hello-app:2.0          # "v2" for rolling-update demos (same code, new tag)
docker build -t notes-app:1.0 apps/notes-app
kind load docker-image hello-app:1.0 hello-app:2.0 notes-app:1.0 --name k8s-tutorial
echo "Loaded images into kind:"
docker exec k8s-tutorial-control-plane crictl images | grep -E 'hello-app|notes-app'
