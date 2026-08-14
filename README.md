# Originate — Hackathon Loan Origination Demo

Originate is a small loan origination system built as the production workload for the TraceFlow hackathon demo. It follows the layered, two-service Spring Boot structure from the supplied bookstore reference while replacing the domain and Angular UI.

## Codebase

```text
backend/
  customer-service/  # Customer profiles and affordability data, port 8081
  loan-service/      # Origination workflow and simulated gateway, port 8082
frontend/            # React + Vite operations UI, port 5173
```

Each backend service uses the same main structure:

```text
controller → service interface/implementation → repository → domain
```

DTOs, centralized exception handling, request correlation, validation, and console logging are kept as separate concerns.

## Workflow

```text
SUBMITTED → SCORED → APPROVED → ACCEPTED → DISBURSED
                    ↘ REJECTED
```

The demo uses in-memory H2 databases so the application can be started without infrastructure. Data resets whenever a service restarts.

## Requirements

- Java 17+
- Node.js 20+

## Run locally

Terminal 1:

```bash
cd backend/customer-service
./gradlew bootRun
```

Terminal 2, healthy mode:

```bash
cd backend/loan-service
./gradlew bootRun
```

Terminal 2, incident-demo mode:

```bash
cd backend/loan-service
DEMO_ERROR_ENABLED=true ./gradlew bootRun
```

Terminal 3:

```bash
cd frontend
npm install
npm run dev
```

Open <http://localhost:5173>. Create an application and move it through scoring, approval, and acceptance. In incident-demo mode, clicking **Disburse funds** returns HTTP 502 and emits the production-like incident to the loan-service console.

## Simulated production incident

`DEMO_ERROR_ENABLED` defaults to `false`. When it is `true`, only the disbursement gateway adapter fails. The failure is deterministic and includes:

- Timestamp, service, thread, and request ID on every log line
- Loan application ID, customer ID, transaction ID, and operation context
- Request start/completion and HTTP status
- Retry evidence
- Full `PaymentGatewayTimeoutException` stack trace
- Nested `SocketTimeoutException` root cause

The application does not create a shortened or AI-processed log. Spring writes the complete exception to stdout/stderr, which is what Kubernetes captures as pod logs. The API error response contains the same request ID so teammates can correlate the UI failure with the full pod log.

No customer email, phone number, or other direct PII is written to the application log.

## Main API endpoints

| Service | Method and path | Purpose |
|---|---|---|
| Customer | `GET /api/customers` | List customers |
| Customer | `POST /api/customers` | Create customer |
| Loan | `GET /api/loan-applications` | List applications |
| Loan | `POST /api/loan-applications` | Submit application |
| Loan | `POST /api/loan-applications/{id}/score` | Run simple scoring |
| Loan | `POST /api/loan-applications/{id}/decision` | Approve or reject |
| Loan | `POST /api/loan-applications/{id}/accept` | Record acceptance |
| Loan | `POST /api/loan-applications/{id}/disburse` | Disburse or trigger demo incident |

## What is intentionally not included yet

GitHub Actions, container images, Kubernetes manifests, Argo CD applications, and log-download/forwarding integration belong to the next DevOps stage after this codebase is reviewed and pushed to GitHub.
