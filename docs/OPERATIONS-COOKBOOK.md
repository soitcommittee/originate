# Originate Operations Cookbook

This cookbook covers the repeatable local GitOps demonstration, incident reproduction, complete log capture, recovery, and teammate access model.

## 1. Start the platform

Verify the cluster and Argo CD:

```bash
kubectl get nodes
kubectl get pods -n argocd
kubectl get applications -n argocd
```

All Argo CD pods should be `Running`. The platform root and every service application should eventually be `Synced` and `Healthy`.

Open Argo CD in terminal 1:

```bash
kubectl port-forward svc/argocd-server -n argocd 8080:443
```

Open <https://localhost:8080>.

Open Originate in terminal 2:

```bash
kubectl port-forward svc/frontend -n originate 3000:80
```

Open <http://localhost:3000>.

Keep both port-forward terminals running.

## 2. Verify the healthy workflow

In the Originate UI:

1. Create a loan application.
2. Run scoring.
3. Approve the application.
4. Record customer acceptance.
5. Disburse the funds.

The final state should be `DISBURSED`.

## 3. Enable the controlled production incident

In `k8s/apps/loan-service/kustomization.yaml`, change:

```yaml
- DEMO_ERROR_ENABLED=false
```

to:

```yaml
- DEMO_ERROR_ENABLED=true
```

Commit and push the change:

```bash
git add k8s/apps/loan-service/kustomization.yaml
git commit -m "Enable disbursement resilience scenario"
git push origin main
```

Argo CD detects the Git change. Kustomize generates a new ConfigMap name, which changes the Loan Service pod template and causes an automatic rollout.

Watch the rollout:

```bash
kubectl rollout status deployment/loan-service -n originate
kubectl get pods -n originate
```

## 4. Reproduce the incident

Because the local profile uses an in-memory database, a restarted Loan Service starts with no previous applications. Create a new application and move it to `ACCEPTED`.

Click **Disburse funds**. The expected result is:

```text
HTTP 502
DISBURSEMENT_GATEWAY_TIMEOUT
```

Copy the Request ID shown in the UI. It connects the browser failure to every related service log line.

## 5. View logs in Argo CD

In Argo CD:

```text
Applications → loan-service → loan-service pod → Logs
```

The log contains the complete request path, retry evidence, exception stack trace, and nested `SocketTimeoutException` root cause.

Each business API call emits one `http_transaction` event containing:

- request ID, method, path, status and duration;
- request and response headers;
- JSON request and response bodies;
- complete masking (`***`) for credentials, cookies and tokens;
- partial masking for names, contact details, identity data, account details and income so records remain correlatable without exposing the full value.

Operational values such as application ID, workflow status, requested amount, product code, credit score and interest rate remain visible. Payload logging is bounded by `HTTP_LOG_MAX_PAYLOAD_BYTES` (8 KiB by default). Health probes use `/actuator/health/*` and are intentionally excluded from business logs.

The probe policy is staged to avoid noisy retries: readiness is checked every 30 seconds, liveness every 60 seconds, and two consecutive liveness failures (about 120 seconds) are required before Kubernetes restarts a container. Kubernetes then applies its own restart backoff. A CronJob is not used for health because health belongs to each running pod and must remain part of Kubernetes lifecycle management.

## 6. Application status batch

`application-status-snapshot` is a Kubernetes CronJob that runs every five minutes. It does not read the database itself. It calls the internal Loan Service endpoint, and Loan Service queries the database it owns. This keeps persistence rules inside the owning service.

Each successful run writes the capture time, total, counts by status, and every application ID/status/update time to the Job log. Borrower and contact data are never included. Failed calls retry after 30, 60 and 120 seconds; `concurrencyPolicy: Forbid` prevents overlapping batches.

Inspect the schedule and recent Jobs:

```bash
kubectl get cronjob application-status-snapshot -n originate
kubectl get jobs -n originate \
  -l app.kubernetes.io/name=application-status-snapshot
```

Trigger it immediately without changing its schedule:

```bash
JOB="application-status-manual-$(date +%s)"
kubectl create job -n originate \
  --from=cronjob/application-status-snapshot "$JOB"
kubectl wait -n originate \
  --for=condition=complete "job/$JOB" \
  --timeout=360s
kubectl logs -n originate "job/$JOB" --timestamps
```

Download that complete batch log to the computer where `kubectl` is running:

```bash
kubectl logs -n originate "job/$JOB" --timestamps \
  > application-status-snapshot.log
```

In Argo CD, open `loan-service`, then select the batch Job/Pod and **Logs**. Up to 24 successful runs (about two hours at this schedule) and six failed runs are retained, with a one-day TTL as a final cleanup guard. Long-term history still belongs in a central log store.

## 7. Capture complete raw logs

Print the complete current Loan Service log:

```bash
kubectl logs -n originate deployment/loan-service \
  --all-containers=true \
  --timestamps
```

Follow the log live:

```bash
kubectl logs -n originate deployment/loan-service \
  --all-containers=true \
  --timestamps \
  --follow
```

Download the complete log without filtering or summarization:

```bash
kubectl logs -n originate deployment/loan-service \
  --all-containers=true \
  --timestamps \
  > loan-service-full.log
```

If the container restarted, retrieve the preceding container instance:

```bash
kubectl logs -n originate deployment/loan-service \
  --all-containers=true \
  --timestamps \
  --previous \
  > loan-service-previous.log
```

Kubernetes keeps only the current and, when available, immediately previous container logs. Long-term production retention requires a central log store.

## 8. Recover and rerun

Change the flag back to healthy mode:

```yaml
- DEMO_ERROR_ENABLED=false
```

Commit and push:

```bash
git add k8s/apps/loan-service/kustomization.yaml
git commit -m "Recover disbursement gateway"
git push origin main
```

Wait for Argo CD to synchronize and Loan Service to roll out:

```bash
kubectl rollout status deployment/loan-service -n originate
```

Create a new application and repeat the workflow. Disbursement should now succeed.

## 9. Reset the local demonstration

The local H2 databases are intentionally ephemeral. Restarting the two backend pods resets the application data and reseeds the customer records:

```bash
kubectl delete pod -n originate \
  -l app.kubernetes.io/name=customer-service

kubectl delete pod -n originate \
  -l app.kubernetes.io/name=loan-service
```

Wait for replacement pods:

```bash
kubectl get pods -n originate -w
```

## 10. CI test and coverage gate

Every backend uses JUnit and JaCoCo. Run the same gate as GitHub Actions locally:

```bash
cd backend/loan-service
./gradlew clean check
```

`check` executes all discovered tests and fails on the first test failure. It also checks every class in the service implementation package independently, requiring at least 70% line coverage and 50% branch coverage. The HTML report is written to:

```text
build/reports/jacoco/test/html/index.html
```

GitHub Actions runs this gate for all four backend services and validates the frontend build before any container image is published. Coverage reports are uploaded as workflow artifacts for 14 days. A failed test or coverage rule prevents the `publish` job from starting.

Coverage is a safety threshold, not proof that every business scenario is correct. Add workflow, validation, error-path and contract tests when adding behavior; do not lower the threshold to make a pipeline green.

## 11. Teammate access

### Human operators

Teammates can use the same Argo CD instance. Each person should receive an individual read-only account with only these permissions:

```text
applications, get, default/*
logs, get, default/*
```

Do not share the `admin` password. Argo CD's `logs, get` permission allows users to view pod logs without permission to sync, delete, execute commands, or change deployments.

The current port-forward exposes Argo CD only on this Mac. Remote teammates require a shared cluster endpoint, an authenticated ingress, VPN access, or a secured tunnel.

### Model and agent integrations

Do not give an automated agent an administrator token. The planned integration is a small read-only Log Access API with Kubernetes permissions limited to:

```text
get/list pods in the originate namespace
get pods/log in the originate namespace
```

The API will return the original log as `text/plain` or as a downloadable file. It will not extract, truncate, summarize, or transform the application log before sending it to the model backend.

Argo CD also supports restricted API accounts and commands such as `argocd app logs loan-service`, but a dedicated log endpoint provides a smaller and clearer permission boundary for teammate agents.
