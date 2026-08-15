# Originate

Originate is a Spring Boot and React loan-origination demo with GitHub container publishing and Argo CD GitOps deployment.

## Services

| Component | Responsibility | Port |
|---|---|---:|
| `customer-service` | Borrower profiles and affordability data | 8081 |
| `loan-service` | Application, scoring, approval and disbursement workflow | 8082 |
| `frontend` | Loan operations portal and API reverse proxy | 80 |

## Run locally

Requirements: Docker Desktop, or Java 17 plus Node.js 20.

```bash
docker compose up --build
```

Open <http://localhost:3000>. Stop with `docker compose down`.

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

For architecture rules and instructions for adding a service, use `.claude/skills/originate-architecture/SKILL.md`.
