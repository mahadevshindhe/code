#!/usr/bin/env bash
# Builds every image used by the modules (no local Java needed - builds run inside Docker).
set -euo pipefail
cd "$(dirname "$0")/.."
docker build -f 02-multistage-builds/Dockerfile.layered -t hello-app:layered apps/hello-app
docker build -f 03-volumes-and-storage/Dockerfile.with-data -t hello-app:data apps/hello-app
docker build -t notes-app:1.0 apps/notes-app
docker images | grep -E 'hello-app|notes-app'
