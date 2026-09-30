# ADR-002: Use Apache Kafka for Asynchronous Events

- Status: Accepted
- Date: 2026-09-29

## Context

Fraud analysis should not be executed synchronously inside the transaction creation HTTP request.

The platform needs to demonstrate asynchronous communication, resilience, retry behavior, duplicate event handling, and service decoupling.

## Decision

Use Apache Kafka as the event broker between the Transaction Service and Fraud Service.

Initial topics:

```text
riskguard.transaction.created.v1
riskguard.fraud.analysis.completed.v1
riskguard.fraud.analysis.dlq.v1
```

## Rationale

Kafka supports the project's learning goals around:

- event-driven systems;
- durable event streams;
- consumer groups;
- replay;
- asynchronous processing;
- failure recovery.

It is also widely used in professional backend and data engineering environments.

## Alternatives Considered

### Direct synchronous HTTP call

Rejected because:

- couples fraud service availability to transaction creation;
- does not demonstrate event-driven architecture;
- creates stronger runtime dependency.

### RabbitMQ

A valid alternative, but Kafka better matches the desired event-streaming and data-oriented portfolio goals.

## Consequences

Positive:

- decoupled fraud processing;
- better resilience to temporary fraud-service downtime;
- event replay capability;
- realistic distributed-system concerns.

Negative:

- additional operational complexity;
- duplicate delivery must be expected;
- event versioning and idempotency become mandatory.

## Constraints

- Consumers must be idempotent.
- Events must be versioned.
- Retries must be bounded.
- Permanent processing failures should use DLQ handling.
- Kafka must not replace PostgreSQL as the source of truth.
