# Monitoring and Lark alerts

## Flow

```text
Spring Boot HTTP metrics
              |
          Prometheus
              |
       alert rule (HTTP 5xx)
              |
         Alertmanager
              |
  notification-service internal API
              |
      service-specific Lark group
```

Prometheus raises `Excessive5xxErrors` when one backend service returns at least three HTTP 5xx responses in five minutes. Service availability is still collected for inspection, but it does not send Lark alerts during normal rolling deployments. Alertmanager groups, deduplicates and retries delivery; resolved notifications are sent when the 5xx error rate recovers.

## 1. Create the Lark bots

In each Lark group, add a **Custom Bot**, copy its webhook URL, and set the bot keyword to `Originate`. Use one group per service. Webhook URLs are credentials: never commit them to Git.

## 2. Create the Kubernetes Secret

Run this on the K3s VM. Inputs are hidden and are not written to shell history.

```bash
read -rsp "customer-service webhook: " CUSTOMER_WEBHOOK; echo
read -rsp "loan-service webhook: " LOAN_WEBHOOK; echo
read -rsp "product-service webhook: " PRODUCT_WEBHOOK; echo
read -rsp "decision-service webhook: " DECISION_WEBHOOK; echo
read -rsp "frontend webhook: " FRONTEND_WEBHOOK; echo
read -rsp "notification-service webhook: " NOTIFICATION_WEBHOOK; echo
read -rsp "platform fallback webhook: " PLATFORM_WEBHOOK; echo

sudo kubectl create secret generic lark-webhook-secrets \
  -n originate \
  --from-literal=customer-service="$CUSTOMER_WEBHOOK" \
  --from-literal=loan-service="$LOAN_WEBHOOK" \
  --from-literal=product-service="$PRODUCT_WEBHOOK" \
  --from-literal=decision-service="$DECISION_WEBHOOK" \
  --from-literal=frontend="$FRONTEND_WEBHOOK" \
  --from-literal=notification-service="$NOTIFICATION_WEBHOOK" \
  --from-literal=platform="$PLATFORM_WEBHOOK" \
  --dry-run=client -o yaml | sudo kubectl apply -f -

unset CUSTOMER_WEBHOOK LOAN_WEBHOOK PRODUCT_WEBHOOK DECISION_WEBHOOK
unset FRONTEND_WEBHOOK NOTIFICATION_WEBHOOK PLATFORM_WEBHOOK
```

The same webhook may be entered more than once while the team is still creating separate groups.

## 3. Verify the deployment

After the GitHub Actions run and Argo CD auto-sync are green:

```bash
sudo kubectl get applications -n argocd
sudo kubectl get pods -n monitoring
sudo kubectl get pods -n originate
sudo kubectl rollout restart deployment/notification-service -n originate
sudo kubectl rollout status deployment/notification-service -n originate --timeout=180s
```

The monitoring namespace should contain Prometheus, Alertmanager and Blackbox Exporter.

## 4. Send a safe routing test

This generates a synthetic Alertmanager payload for `loan-service`; it does not create a loan error.

```bash
sudo kubectl run lark-alert-test \
  --rm -i --restart=Never \
  --image=curlimages/curl:8.12.1 \
  -n originate -- \
  curl --fail-with-body --silent --show-error \
  -H 'Content-Type: application/json' \
  -d '{"status":"firing","commonLabels":{"alertname":"ManualRoutingTest","service":"loan-service","severity":"warning"},"commonAnnotations":{"summary":"Manual Lark routing test","description":"Monitoring path is operational."},"alerts":[{"status":"firing","labels":{"alertname":"ManualRoutingTest","service":"loan-service","severity":"warning"},"annotations":{"summary":"Manual Lark routing test","description":"Monitoring path is operational."},"startsAt":"2026-08-16T00:00:00Z","generatorURL":"manual-test"}]}' \
  http://notification-service:8085/api/alerts/alertmanager
```

Expected response: `"delivered":true`, followed by a message in the loan-service Lark group.

## 5. Inspect alerts

The dashboards remain private inside K3s. Use SSH port-forwarding only when needed:

```bash
sudo kubectl port-forward -n monitoring service/prometheus 9090:9090
sudo kubectl port-forward -n monitoring service/alertmanager 9093:9093
```

Then open `http://127.0.0.1:9090/alerts` or `http://127.0.0.1:9093` through the VM's SSH tunnel. Application logs remain available in Argo CD and through `kubectl logs`.

## Alert configuration

- Rules: `k8s/apps/monitoring/alert-rules.yml`
- Prometheus targets: `k8s/apps/monitoring/prometheus.yml`
- Grouping and retry: `k8s/apps/monitoring/alertmanager.yml`
- Per-group delivery API: `POST /api/alerts/alertmanager` on `notification-service`

Do not expose Prometheus, Alertmanager, or `notification-service` with a public NodePort.
