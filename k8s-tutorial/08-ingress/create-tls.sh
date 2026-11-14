#!/usr/bin/env bash
# Creates a self-signed certificate and stores it as a TLS secret in namespace demo.
set -euo pipefail
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout /tmp/tls.key -out /tmp/tls.crt \
  -subj "/CN=secure.localtest.me/O=k8s-tutorial" \
  -addext "subjectAltName=DNS:secure.localtest.me"
kubectl -n demo create secret tls secure-tls --cert=/tmp/tls.crt --key=/tmp/tls.key --dry-run=client -o yaml | kubectl apply -f -
echo "Secret secure-tls created"
