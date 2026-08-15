# GitOps conventions

## Ownership mapping

```text
backend/customer-service
  -> GHCR originate-customer-service
  -> k8s/apps/customer-service
  -> argocd/apps/customer-service.yaml
  -> Argo Application customer-service
```

Apply the same one-to-one mapping to every deployable. The root `originate-platform` Application owns only child Application resources under `argocd/apps`; it never owns service workloads directly.

## Validation

Run backend tests with Java 17:

```bash
cd backend/<service>
./gradlew test
```

Render every service independently:

```bash
kubectl kustomize k8s/apps/customer-service >/dev/null
kubectl kustomize k8s/apps/database >/dev/null
kubectl kustomize k8s/apps/loan-service >/dev/null
kubectl kustomize k8s/apps/frontend >/dev/null
```

Validate client-side schemas when the cluster is reachable:

```bash
kubectl apply --dry-run=client -f argocd/bootstrap/root-application.yaml
kubectl apply --dry-run=client -f argocd/apps/
```

## Image release

1. Push source changes and wait for every GHCR matrix job to succeed.
2. Copy the successful full commit SHA.
3. Change only that service's image to `sha-<full-commit>` under `k8s/apps/<service>`.
4. Commit the manifest-only change. Deployment-only paths are ignored by the image build workflow.

## Migration from the legacy aggregate app

Delete the old `originate` Application without cascading its workloads, then apply the root Application. Sync the root and allow child Applications to adopt the existing resources. Confirm all children are healthy before removing obsolete files.

## Secrets

The private repository credential stays in Argo CD. The `ghcr-pull-secret` and `originate-database-credentials` stay in namespace `originate`. Never store tokens or passwords in Git. A child Application can reference a secret but must not own or prune it.
