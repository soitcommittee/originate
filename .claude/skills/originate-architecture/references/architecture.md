# Originate domain architecture

## Implemented flow

```text
React frontend
  |-- /api/customers --------> customer-service
  `-- /api/loan-applications -> loan-service
                                  `-> customer-service
```

`customer-service` owns borrower and affordability data. `loan-service` currently owns the compact origination workflow: submission, scoring, underwriting decision, acceptance and disbursement. The frontend is an operations UI and Nginx reverse proxy; it does not own business rules.

## Target service map

Evolve the compact demo toward these bounded contexts without changing repositories:

| Stage | Service | Ownership |
|---|---|---|
| Pre-loan | `customer-service` | Profile, KYC, employment and bank details |
| Pre-loan | `application-service` | Application state and submitted facts |
| Pre-loan | `product-service` | Product limits, rates, fees and rules |
| Pre-loan | `decision-service` | Scorecards, affordability and approve/reject result |
| Active loan | `loan-account-service` | Principal, balance and account status |
| Active loan | `schedule-service` | Installments, due dates and allocation schedule |
| Repayment | `payment-service` | Payment attempts, reversals and gateway results |
| Repayment | `ledger-service` | Immutable accounting entries |
| Repayment | `reconciliation-service` | Internal-to-bank transaction matching |
| Post-loan | `delinquency-service` | Days past due and delinquency classification |
| Post-loan | `restructure-service` | Reschedule, settlement and write-off requests |
| Post-loan | `collection-service` | Collector work, promises to pay and legal handoff |

Supporting contexts may include notification, document, audit, workflow, fraud, integration and reporting services.

## Dependency direction

```text
customer -> application -> decision -> loan-account -> schedule
                           product          |
                                            v
payment -> reconciliation -> ledger -> loan-account
                                            |
                                            v
delinquency -> restructure -> collection -> legal/write-off
```

Avoid shared databases. Integrate through versioned HTTP contracts initially. Introduce Kafka only for durable cross-service events that have clear ownership and retry/idempotency requirements.

## Extraction order

Extract from the current `loan-service` incrementally:

1. Product rules into `product-service`.
2. Scoring and underwriting into `decision-service`.
3. Post-disbursement truth into `loan-account-service` and `schedule-service`.
4. Repayment, ledger and reconciliation.
5. Delinquency, restructure and collection.

Keep the existing user workflow working after every extraction. Add contract tests before moving logic.
