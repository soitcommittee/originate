# Originate — Loan Origination Platform

Originate is a digital loan-origination platform that manages an application from initial submission through credit scoring, underwriting, customer acceptance, and fund disbursement.

The platform provides operations teams with a central workspace for reviewing applications, recording decisions, tracking workflow status, and investigating service failures through correlated application logs.

## Core capabilities

- Customer profile and affordability management
- Loan application submission and validation
- Automated credit scoring and indicative interest rates
- Underwriter approval and rejection decisions
- Customer offer acceptance
- Disbursement processing
- Request correlation across services
- Structured operational logs with complete exception details

## Architecture

```text
React operations portal
          │
          ├── Customer Service :8081
          │     └── Customer profiles and income information
          │
          └── Loan Service :8082
                ├── Origination workflow
                ├── Credit scoring
                └── Disbursement gateway adapter
```

Repository layout:

```text
backend/
  customer-service/  # Customer profiles and affordability data
  loan-service/      # Application workflow, scoring, and disbursement
frontend/            # React operations portal
```

Both backend services use a layered Spring Boot design:

```text
controller → service → repository → domain
```

Request DTOs, response DTOs, validation, exception handling, and request logging are separated from the business domain.

## Application lifecycle

```text
SUBMITTED → SCORED → APPROVED → ACCEPTED → DISBURSED
                    ↘ REJECTED
```

| Stage | Description |
|---|---|
| Submitted | The customer's application has been received. |
| Scored | Affordability and credit rules have generated a score and indicative rate. |
| Approved/Rejected | An underwriting decision has been recorded. |
| Accepted | The customer has accepted the approved offer. |
| Disbursed | Funds have been transferred successfully. |

## Technology stack

- Java 17 and Spring Boot 3.5
- Spring Data JPA and OpenFeign
- React 19 and Vite
- H2 for the local development profile
- Gradle 8.14

The local profile uses independent in-memory databases so the platform starts without external infrastructure. Local data is reset when a service restarts. Production environments can provide persistent database configuration through Spring environment variables.

## Local setup

Requirements:

- Java 17 or newer
- Node.js 20 or newer

On macOS, select Java 17 in every backend terminal before starting Gradle:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

### 1. Start Customer Service

```bash
cd backend/customer-service
./gradlew bootRun
```

Customer Service will be available at <http://localhost:8081>.

### 2. Start Loan Service

Open a second terminal and select Java 17 again:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH="$JAVA_HOME/bin:$PATH"

cd backend/loan-service
./gradlew bootRun
```

Loan Service will be available at <http://localhost:8082>.

### 3. Start the operations portal

```bash
cd frontend
npm install
npm run dev
```

Open <http://localhost:5173> and create a loan application. The application can then be scored, approved or rejected, accepted, and disbursed from the workspace.

## Observability

Every incoming API request receives an `X-Request-ID`. The identifier is returned to the caller, propagated between services, and included in every related log line.

Operational logs include:

- Timestamp, service name, thread, log level, and request ID
- Workflow and downstream-operation context
- Loan application, customer, and transaction identifiers
- HTTP result and request duration
- Complete exception stack traces and nested root causes

Logs are written directly to standard output. Container platforms such as Kubernetes can therefore capture the original service output without application-side summarization or extraction. Direct customer PII such as email addresses and phone numbers is excluded from operational logs.

## Resilience test mode

Loan Service includes a controlled resilience scenario for validating incident monitoring and recovery workflows. Enable it only in a test environment:

```bash
cd backend/loan-service
DEMO_ERROR_ENABLED=true ./gradlew bootRun
```

With this setting enabled, the disbursement gateway returns a simulated read timeout after the application reaches `ACCEPTED`. Loan Service responds with HTTP `502 Bad Gateway`, preserves the application in its pre-disbursement state, and writes the full incident stack trace to standard output.

The response contains the request ID needed to locate the complete transaction across Customer Service and Loan Service logs.

## API endpoints

| Service | Method and path | Purpose |
|---|---|---|
| Customer | `GET /api/customers` | List customer profiles |
| Customer | `GET /api/customers/{id}` | Retrieve a customer profile |
| Customer | `POST /api/customers` | Create a customer profile |
| Loan | `GET /api/loan-applications` | List loan applications |
| Loan | `GET /api/loan-applications/{id}` | Retrieve an application |
| Loan | `POST /api/loan-applications` | Submit an application |
| Loan | `POST /api/loan-applications/{id}/score` | Run credit scoring |
| Loan | `POST /api/loan-applications/{id}/decision` | Record an underwriting decision |
| Loan | `POST /api/loan-applications/{id}/accept` | Record customer acceptance |
| Loan | `POST /api/loan-applications/{id}/disburse` | Initiate fund disbursement |

## Planned deployment components

- Container images for each service
- Kubernetes workload and service manifests
- GitHub Actions build and image-publishing pipeline
- Argo CD application definitions
- Full pod-log retrieval and forwarding integration
