# Module 21 - Observability: logs, events, metrics, dashboards

**Concepts:** `kubectl logs/events/top`, metrics-server vs Prometheus, Prometheus Operator
(`ServiceMonitor` CRD), Grafana, Spring Boot Actuator/Micrometer, RED/USE metrics, logging architecture.

## 1. Built-in tooling (no installs)
```bash
kubectl get events -A --sort-by=.lastTimestamp | tail -20
kubectl get events -n <ns> --field-selector type=Warning
kubectl logs deploy/<name> -n <ns> --tail=50 -f
kubectl logs -l app=hello -n <ns> --prefix --since=10m           # all replicas, prefixed with pod/container
kubectl logs <pod> --previous                                     # crashed container
kubectl top nodes; kubectl top pods -A --sort-by=memory           # metrics-server
kubectl get --raw /metrics | head                                 # API server metrics (needs permissions)
kubectl describe node k8s-tutorial-worker | sed -n '/Allocated resources/,/Events/p'
```
Better log UX: `stern` (`stern -n notes notes-app`), `k9s` (terminal UI), Lens / Headlamp.

## 2. Prometheus + Grafana (kube-prometheus-stack via Helm)
```bash
cd 21-observability
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts && helm repo update
kubectl create namespace monitoring
helm install kps prometheus-community/kube-prometheus-stack -n monitoring -f kps-values.yaml
kubectl -n monitoring get pods -w            # wait for Running (several minutes, ~1.5GB RAM)
```

## 3. Monitor the Java app
```bash
kubectl apply -f hello-monitored.yaml
kubectl -n monitoring port-forward svc/kps-kube-prometheus-stack-prometheus 9090:9090 &
# open http://localhost:9090 -> Status > Targets: serviceMonitor/monitoring-demo/hello (2 targets UP)
# queries: jvm_memory_used_bytes{area="heap"}   rate(http_server_requests_seconds_count[1m])   process_cpu_usage

kubectl -n monitoring port-forward svc/kps-grafana 3000:80 &
# open http://localhost:3000  (admin / admin) -> Dashboards: "Kubernetes / Compute Resources / Namespace (Pods)"
# Import dashboard ID 4701 (JVM Micrometer) for Java metrics
```
Generate traffic: `kubectl -n monitoring-demo run l --rm -it --image=curlimages/curl --restart=Never -- sh -c 'for i in $(seq 1 300); do curl -s hello/hello >/dev/null; done'`

## 4. Alert example (PrometheusRule)
```yaml
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata: { name: hello-alerts, namespace: monitoring-demo }
spec:
  groups:
    - name: hello
      rules:
        - alert: HelloHighHeap
          expr: sum(jvm_memory_used_bytes{area="heap",namespace="monitoring-demo"}) / sum(jvm_memory_max_bytes{area="heap",namespace="monitoring-demo"}) > 0.85
          for: 5m
          labels: { severity: warning }
```

## 5. Production observability architecture
| Signal | Typical stack |
|---|---|
| Metrics | Prometheus (+Thanos/Mimir for long-term), Grafana |
| Logs | stdout -> Fluent Bit/Vector (DaemonSet) -> Loki / Elasticsearch |
| Traces | OpenTelemetry (Java agent or Micrometer Tracing) -> Tempo / Jaeger |
| Events/alerts | Alertmanager -> Slack / PagerDuty |
Golden signals per service: **R**ate, **E**rrors, **D**uration (RED); per resource: **U**tilisation, **S**aturation, **E**rrors (USE).

## Cleanup
```bash
kill %1 %2 2>/dev/null
helm uninstall kps -n monitoring; kubectl delete namespace monitoring monitoring-demo
kubectl get crd -o name | grep monitoring.coreos.com | xargs -r kubectl delete     # CRDs are left behind by Helm
```
