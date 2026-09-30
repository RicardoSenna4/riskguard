# AGENTS.md

## Purpose

This file defines mandatory rules for AI agents and automated coding assistants working on the RiskGuard repository.

The agent is an implementation assistant. Architectural decisions, domain rules, security decisions, and scope changes remain human responsibilities.

---

## Before Editing

Before changing any file:

1. Read this file completely.
2. Read the task description.
3. Read only the project documentation relevant to the task.
4. Inspect the files directly related to the requested change.
5. Explain the current behavior briefly.
6. Present a short implementation plan.
7. List the files you expect to change.
8. Identify risks or ambiguities.
9. Do not modify any file until the task scope is understood.

---

## Project Architecture

The approved architecture is:

- Java 21 + Spring Boot for the transaction service.
- Python + FastAPI for the fraud service.
- React + TypeScript for the frontend.
- PostgreSQL as the primary source of truth.
- Redis only for cache, rate limiting, and temporary idempotency support.
- Apache Kafka for asynchronous communication.
- Transactional Outbox for reliable event publication.
- Docker Compose for local infrastructure.
- GitHub Actions for CI.
- Prometheus and Grafana for observability.

The agent must not replace these technologies without explicit approval.

---

## Architectural Rules

1. PostgreSQL is the system of record.
2. Redis must never become the primary source of truth.
3. Events must be versioned.
4. Kafka consumers must be idempotent.
5. Transaction creation and event creation must use the Transactional Outbox pattern.
6. Business rules belong in the domain/service layer, not in controllers.
7. Controllers must remain thin.
8. JPA entities must not be exposed directly through the public API.
9. DTOs must define API contracts.
10. Money must use `BigDecimal`, `Decimal`, or `NUMERIC`, never floating-point types.
11. Dates and timestamps must be stored in UTC.
12. Public API breaking changes require versioning.
13. Distributed operations must consider retry, duplicate delivery, partial failure, and idempotency.

---

## Restrictions

The agent must NOT:

- introduce new dependencies without explicit approval;
- change the architecture without an ADR and approval;
- refactor unrelated modules;
- rename public endpoints without approval;
- change event schemas without approval;
- modify authentication rules outside the task scope;
- remove or weaken security controls;
- disable tests to make the build pass;
- delete failing tests without justification;
- access production systems;
- create real secrets;
- commit credentials, tokens, certificates, or passwords;
- use production databases;
- execute destructive commands outside the repository;
- rewrite large portions of working code when a smaller change is sufficient;
- perform unsolicited cleanup or “improvements”;
- silently change business rules.

---

## Scope Discipline

Each task must be treated as a small, isolated feature or fix.

If the task says:

> Implement transaction creation.

The agent must not also implement:

- Kafka;
- Outbox;
- Redis;
- ML;
- frontend;
- unrelated refactors.

Changes outside the requested scope require explicit approval.

---

## Testing Rules

Every behavior change must be protected by automated tests.

Prefer:

1. define expected behavior;
2. write or update tests;
3. implement the minimum required code;
4. run tests;
5. refactor only after tests pass.

Required test levels when applicable:

- unit tests;
- repository tests;
- integration tests;
- API tests;
- Kafka integration tests;
- frontend tests;
- end-to-end tests.

Never rely only on compilation.

---

## Validation After Editing

After each task, run all relevant checks.

### Java

```bash
mvn test
mvn verify
```

Run configured formatter/linter if available.

### Python

```bash
ruff check .
mypy .
pytest
```

### Frontend

```bash
npm run lint
npm test
npm run build
```

Only run commands that already exist in the project configuration.

---

## Dependency Policy

Before introducing a new dependency, provide:

- problem being solved;
- why existing code cannot solve it adequately;
- proposed dependency;
- alternatives considered;
- maintenance/security cost;
- final recommendation.

Do not add a dependency until approved.

---

## Security Rules

Never expose in logs:

- passwords;
- complete JWTs;
- refresh tokens;
- API keys;
- private keys;
- full sensitive documents;
- secret environment variables.

Security-sensitive changes require special review.

Do not reduce authorization or validation merely to make tests pass.

---

## Database Rules

- All schema changes must use migrations.
- Applied migrations must not be edited retroactively.
- Use foreign keys when appropriate.
- Use unique constraints for real uniqueness requirements.
- Add indexes based on actual query patterns.
- Avoid application-only validation when a database constraint can enforce a critical invariant.
- No destructive migration without explicit approval.

---

## Kafka Rules

Every event must contain:

- `eventId`;
- `eventType`;
- `eventVersion`;
- `occurredAt`;
- `correlationId`;
- `data`.

Consumers must tolerate duplicate delivery.

Permanent failures should go to a DLQ when configured.

Retries must be bounded.

---

## API Rules

REST endpoints must use:

```text
/api/v1
```

Error responses must be standardized.

Do not return stack traces to API clients.

Use appropriate HTTP status codes.

---

## Code Quality

Prefer:

- simple code;
- explicit domain language;
- small methods;
- cohesive classes;
- clear names;
- minimal abstractions;
- composition where appropriate;
- focused commits.

Avoid:

- generic base services without real need;
- giant controllers;
- giant services;
- speculative abstraction;
- premature optimization;
- hidden side effects.

---

## After Editing

When implementation is complete:

1. Run relevant tests.
2. Run lint/static checks.
3. Run the build.
4. List every changed file.
5. Explain why each file changed.
6. Report commands executed.
7. Report test/build results honestly.
8. Report any unresolved issue.
9. Do not make further changes unless requested.

---

## Required Final Report Format

```markdown
## Summary
What was implemented.

## Files Changed
- path/file: reason

## Tests
- command
- result

## Build
- command
- result

## Risks / Remaining Issues
- none, or explicit items

## Out of Scope
Anything intentionally not implemented.
```
