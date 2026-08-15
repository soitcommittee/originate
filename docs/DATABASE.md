# Database operations

Originate runs one PostgreSQL instance with separate `customer_service` and `loan_service` schemas. Service code remains schema-owned and cross-service communication continues through APIs.

## First Kubernetes deployment

Run this once on the K3s VM before pushing the database manifests:

```bash
sudo kubectl create namespace originate --dry-run=client -o yaml | sudo kubectl apply -f -

read -rsp "Database password: " DB_PASSWORD
echo
sudo kubectl create secret generic originate-database-credentials \
  -n originate \
  --from-literal=username=originate \
  --from-literal=password="$DB_PASSWORD" \
  --from-literal=database=originate \
  --dry-run=client -o yaml | sudo kubectl apply -f -
unset DB_PASSWORD
```

Do not commit the password or generated Secret YAML to Git.

## Verify persistence

```bash
sudo kubectl get statefulset,pvc,pod -n originate \
  -l app.kubernetes.io/name=originate-postgres

sudo kubectl exec -n originate statefulset/originate-postgres -- \
  psql -U originate -d originate -c '\dn'
```

The first empty database receives twelve demo customers and twelve applications across submitted, scored, approved, accepted, disbursed and rejected stages. Later pod restarts retain all records in the persistent volume.

## Local reset

Local Docker data also persists. To intentionally reset it and reload the demo portfolio:

```bash
docker compose down -v
docker compose up --build
```
