# Originate

Originate is a Spring Boot and React loan-origination demo with GitHub container publishing and Argo CD GitOps deployment.

## Services

| Component | Responsibility | Port |
|---|---|---:|
| `customer-service` | Borrower profiles and affordability data | 8081 |
| `loan-service` | Application, approval, acceptance and disbursement workflow | 8082 |
| `product-service` | Loan product limits, base rates and tenures | 8083 |
| `decision-service` | Affordability scoring and credit recommendations | 8084 |
| `notification-service` | Alertmanager routing to per-service Lark groups and Haaland debugging | 8085 |
| `originate-postgres` | Persistent customer and loan portfolio data | 5432 |
| `frontend` | Loan operations portal and API reverse proxy | 80 |

## Run locally

Requirements: Docker Desktop, or Java 17 plus Node.js 20.

```bash
docker compose up --build
```

Open <http://localhost:3000>. Stop with `docker compose down`. Customer and application data remains in the `originate-postgres-data` Docker volume.

Enable the controlled disbursement failure with:

```bash
DEMO_ERROR_ENABLED=true docker compose up --build
```

## Repository layout

```text
backend/                       Spring Boot services
frontend/                      React operations portal
k8s/apps/<service>/            One deployable Kustomize directory per service
argocd/apps/                   One Argo CD child Application per service
argocd/bootstrap/              Root App-of-Apps bootstrap
.claude/skills/                Architecture and change conventions
```

All services stay in this monorepo. Argo CD tracks each service directory independently, so a new microservice does not require a new GitHub repository.

## Deployment

Images are published to GHCR with immutable `sha-<commit>` tags. Bootstrap the platform with:

```bash
kubectl apply -f argocd/bootstrap/root-application.yaml
```

The root application discovers the child definitions in `argocd/apps/`. Each child syncs only its matching `k8s/apps/<service>` directory.

For incident reproduction, log capture and recovery commands, see [docs/OPERATIONS-COOKBOOK.md](docs/OPERATIONS-COOKBOOK.md).

Before the first Kubernetes deployment, create the database credential secret by following [docs/DATABASE.md](docs/DATABASE.md). PostgreSQL then keeps service data on a 5 Gi persistent volume.

Prometheus monitors backend HTTP errors and service health, Alertmanager groups repeated incidents, and `notification-service` sends each service's alert to its own Lark group. Setup and test commands are in [docs/MONITORING-ALERTING.md](docs/MONITORING-ALERTING.md).

GitHub Actions runs every backend test plus the JaCoCo service-layer coverage gate before publishing images. The Loan Service deployment also includes a five-minute application-status snapshot CronJob; its operation and manual trigger commands are in the cookbook.

For architecture rules and instructions for adding a service, use `.claude/skills/originate-architecture/SKILL.md`.
