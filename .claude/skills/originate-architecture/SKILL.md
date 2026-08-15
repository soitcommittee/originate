---
name: originate-architecture
description: Maintain the Originate loan-platform monorepo and its Spring Boot, React, Docker, Kubernetes, GHCR, and Argo CD App-of-Apps architecture. Use when adding, splitting, renaming, integrating, deploying, or troubleshooting an Originate microservice; changing service APIs or dependencies; updating container publishing; or modifying k8s/apps and argocd/apps.
---

# Originate Architecture

Preserve independently deployable services inside one repository. Keep code, image, Kubernetes ownership, and Argo CD ownership aligned by service name.

## Required reading

- Read [references/architecture.md](references/architecture.md) before changing service boundaries or request flows.
- Read [references/gitops.md](references/gitops.md) before adding, renaming, deploying, or deleting a service.

## Invariants

1. Keep one business responsibility and datastore boundary per backend service.
2. Follow the existing Spring Boot layering: `controller -> service -> repository -> domain` with separate DTO, exception, and config packages.
3. Emit structured logs to stdout and propagate `X-Request-ID` across HTTP calls.
4. Never log credentials, tokens, identity numbers, phone numbers, or raw customer documents.
5. Put each deployable at `k8s/apps/<service>` and each Argo child at `argocd/apps/<service>.yaml`.
6. Point each child Application only at its matching service directory. Never recreate a combined `k8s/base` application.
7. Use immutable GHCR `sha-<full-commit>` image tags in Kubernetes.
8. Keep runtime secrets outside Git. Reference Kubernetes Secrets by name.
9. Add health probes and CPU/memory requests and limits to every workload.
10. Test the affected backend, render its Kustomize directory, and validate the root App-of-Apps before committing.

## Add a service

1. Create `backend/<service>` by following an existing Gradle/Spring Boot service structure.
2. Add its Docker build context to `.github/workflows/publish-images.yml`.
3. Add `k8s/apps/<service>/deployment.yaml` and `kustomization.yaml`.
4. Add `argocd/apps/<service>.yaml` with automated prune and self-heal.
5. Add only necessary service-to-service URLs through a service-owned ConfigMap.
6. Update the architecture reference and the short service table in the root README.
7. Run tests and render checks described in the GitOps reference.

## Change an API

Update the provider contract, consumer client, request-ID propagation, tests, reverse proxy routes when browser-facing, and the architecture flow together. Prefer backward-compatible response additions. Do not silently repurpose an existing field or status.

## Controlled incident

Keep `DEMO_ERROR_ENABLED` disabled by default. Scope the simulated failure to the downstream disbursement adapter, return a stable error code and request ID, preserve the pre-disbursement state, and emit the complete exception chain to stdout.
