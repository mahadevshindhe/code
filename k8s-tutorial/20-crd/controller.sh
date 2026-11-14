#!/usr/bin/env bash
# A deliberately tiny "operator": a reconcile loop written in bash.
# It watches HelloApp objects and makes sure a matching Deployment (+status) exists.
# Real operators do exactly this using client-go / controller-runtime / Java Operator SDK / Quarkus Operator SDK.
set -u
NS=${NS:-crd-demo}
echo "Controller started (namespace=$NS). Ctrl+C to stop."
while true; do
  for name in $(kubectl -n "$NS" get helloapps -o jsonpath='{.items[*].metadata.name}'); do
    replicas=$(kubectl -n "$NS" get helloapp "$name" -o jsonpath='{.spec.replicas}')
    message=$(kubectl -n "$NS" get helloapp "$name" -o jsonpath='{.spec.message}')
    uid=$(kubectl -n "$NS" get helloapp "$name" -o jsonpath='{.metadata.uid}')

    # DESIRED STATE: a Deployment owned by the custom resource (ownerReferences => garbage-collected with it)
    cat <<YAML | kubectl -n "$NS" apply -f - >/dev/null
apiVersion: apps/v1
kind: Deployment
metadata:
  name: $name
  ownerReferences:
    - { apiVersion: tutorial.example.com/v1, kind: HelloApp, name: $name, uid: "$uid", controller: true, blockOwnerDeletion: true }
spec:
  replicas: $replicas
  selector: { matchLabels: { helloapp: $name } }
  template:
    metadata: { labels: { helloapp: $name } }
    spec:
      containers:
        - name: hello
          image: hello-app:1.0
          imagePullPolicy: IfNotPresent
          env: [{ name: APP_GREETING, value: "$message" }]
          readinessProbe: { httpGet: { path: /actuator/health/readiness, port: 8080 }, initialDelaySeconds: 10 }
          resources: { requests: { cpu: 50m, memory: 256Mi } }
YAML

    # OBSERVED STATE -> status subresource
    ready=$(kubectl -n "$NS" get deploy "$name" -o jsonpath='{.status.readyReplicas}')
    kubectl -n "$NS" patch helloapp "$name" --subresource=status --type=merge -p "{\"status\":{\"readyReplicas\":${ready:-0}}}" >/dev/null
  done
  sleep 5
done
