# Module 15 - Network Policies (pod firewalling)

**Concepts:** default-allow networking, NetworkPolicy semantics (additive allow-lists), ingress/egress,
pod/namespace selectors, default-deny, DNS egress gotcha, CNI requirement.

> **Requires a CNI that enforces NetworkPolicy.** kind's default (kindnet) does NOT (newer kindnet builds may).
> Recommended: recreate the cluster with Calico:
> ```bash
> ./scripts/delete-cluster.sh && ./scripts/create-cluster.sh --calico && ./scripts/build-and-load-images.sh
> ```
> (minikube: `minikube start --cni=calico`.)

```bash
cd 15-network-policies
kubectl apply -f app.yaml
kubectl -n netpol-demo wait --for=condition=ready pod --all
```

## 1. Baseline: everything can talk to everything
```bash
kubectl -n netpol-demo exec frontend -- wget -qO- -T 3 http://backend | head -2     # works
kubectl -n netpol-demo exec intruder -- wget -qO- -T 3 http://db | head -2          # works (bad!)
```

## 2. Apply zero-trust policies
```bash
kubectl apply -f policies.yaml
kubectl -n netpol-demo get netpol
```

## 3. Test matrix
```bash
t() { kubectl -n netpol-demo exec $1 -- wget -qO- -T 3 http://$2 >/dev/null 2>&1 && echo "$1 -> $2 : ALLOWED" || echo "$1 -> $2 : BLOCKED"; }
t frontend backend      # ALLOWED
t intruder backend      # BLOCKED
t frontend db           # BLOCKED
t backend db            # ALLOWED
t intruder db           # BLOCKED
```

## 4. Egress policy
```bash
kubectl apply -f egress-policy.yaml
kubectl -n netpol-demo exec frontend -- wget -qO- -T 3 http://example.com >/dev/null && echo allowed || echo "internet BLOCKED"
t frontend backend      # still ALLOWED (DNS egress rule makes name resolution work)
```
Forgetting the DNS egress rule is the #1 cause of "everything broke after adding egress policy".

## Semantics to remember
- Pods selected by **no** policy: fully open. Once **any** policy selects a pod for a direction, only listed traffic is allowed.
- Policies are additive (union); there are no deny rules (Calico/Cilium offer extended CRDs with deny).
- `from: [{podSelector}, {namespaceSelector}]` as separate list items = OR; in the same item = AND.
- Ingress controller / monitoring namespaces must be explicitly allowed once default-deny is on.

## Cleanup
```bash
kubectl delete namespace netpol-demo
```
