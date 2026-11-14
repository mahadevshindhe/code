# Module 13 - Jobs, CronJobs, DaemonSets

**Concepts:** run-to-completion workloads, retries/backoff, parallelism & indexed jobs, TTL cleanup,
CronJob schedule/concurrency/history, DaemonSet (one pod per node), tolerations for control-plane.

```bash
cd 13-jobs-cronjobs-daemonsets
kubectl create namespace demo 2>/dev/null
```

## Jobs
```bash
kubectl apply -f job.yaml
kubectl -n demo get jobs,pods -w
kubectl -n demo logs job/pi-job
kubectl -n demo wait --for=condition=complete job/pi-job --timeout=60s
kubectl -n demo logs -l job-name=parallel-job --tail=2          # chunks 0..5, 2 pods at a time
kubectl -n demo get pods -l job-name=failing-job                # several Error pods (backoffLimit)
kubectl -n demo describe job failing-job | tail -8              # "BackoffLimitExceeded"
```

## CronJob
```bash
kubectl apply -f cronjob.yaml
kubectl -n demo get cronjob
kubectl -n demo get jobs -w                                      # a new Job every minute
kubectl -n demo logs -l job-name --tail=1 --prefix | tail -3
kubectl -n demo create job manual-run --from=cronjob/every-minute      # trigger manually
kubectl -n demo patch cronjob every-minute -p '{"spec":{"suspend":true}}'   # pause
```

## DaemonSet
```bash
kubectl apply -f daemonset.yaml
kubectl -n demo get ds,pods -l app=node-agent -o wide            # 3 pods (control-plane + 2 workers)
kubectl -n demo logs -l app=node-agent --prefix --tail=1
kubectl -n demo rollout status ds/node-agent
```
Add a new node (`kind` multi-node) and the DaemonSet pod appears automatically.

## When to use what
| Need | Resource |
|---|---|
| Stateless long-running | Deployment |
| Stable identity/storage | StatefulSet |
| One per node | DaemonSet |
| Run once to completion | Job |
| Scheduled | CronJob |

Java batch (Spring Batch) -> Job with `restartPolicy: Never` + idempotent steps + `activeDeadlineSeconds`.

## Cleanup
```bash
kubectl delete namespace demo
```
