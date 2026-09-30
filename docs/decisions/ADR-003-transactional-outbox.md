# ADR-003: Use the Transactional Outbox Pattern

- Status: Accepted
- Date: 2026-09-29

## Context

When creating a transaction, the system must both:

1. persist the transaction in PostgreSQL;
2. publish `transaction.created.v1` to Kafka.

A naive flow:

```text
INSERT transaction
→ publish Kafka event
```

can fail between these operations.

Examples:

- database commit succeeds but Kafka publication fails;
- Kafka publication succeeds but database transaction rolls back.

That creates inconsistent state.

## Decision

Use the Transactional Outbox pattern.

Within the same PostgreSQL transaction:

```text
BEGIN

INSERT transaction
INSERT outbox_event

COMMIT
```

A separate publisher then reads pending outbox records and publishes them to Kafka.

## Rationale

This guarantees that the transaction record and intent to publish the event are committed atomically.

## Alternatives Considered

### Database then Kafka directly

Rejected due to the dual-write consistency problem.

### Kafka transaction as the source of truth

Rejected because PostgreSQL remains the platform's system of record.

### Distributed transaction / 2PC

Rejected because it adds excessive complexity for this project.

## Consequences

Positive:

- no transaction can be committed without an associated event intent;
- Kafka outages do not lose transaction events;
- retries are possible.

Negative:

- additional table and publisher process;
- duplicate Kafka publication is still possible;
- consumers must remain idempotent.

## Constraints

The outbox record must include:

- event ID;
- aggregate ID;
- event type;
- payload;
- status;
- retry count;
- created timestamp;
- published timestamp.

A message must only be marked published after successful broker acknowledgement.
