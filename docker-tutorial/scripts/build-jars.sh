#!/usr/bin/env bash
# Builds both apps locally (needs JDK 21 + Maven). Only required for module 01, 11, 16.
set -euo pipefail
cd "$(dirname "$0")/.."
mvn -f apps/hello-app/pom.xml clean package -DskipTests
mvn -f apps/notes-app/pom.xml clean package -DskipTests
echo "Built: apps/hello-app/target/app.jar and apps/notes-app/target/app.jar"
