# ADR-004: Implement the Fraud Service in Python

- Status: Accepted
- Date: 2026-09-29

## Context

RiskGuard requires a component responsible for:

- consuming transaction events;
- feature engineering;
- rule-based risk analysis;
- machine-learning inference;
- publishing fraud decisions.

The Transaction Service already uses Java/Spring Boot.

The fraud component has different technical needs, especially around data science and machine learning.

## Decision

Implement the Fraud Service using Python and FastAPI.

Approved core stack:

- Python 3.12+;
- FastAPI;
- Pydantic;
- SQLAlchemy;
- Alembic;
- Pandas;
- NumPy;
- Scikit-learn;
- Pytest;
- Ruff;
- MyPy.

## Rationale

Python provides mature libraries for:

- data processing;
- feature engineering;
- model training;
- model evaluation;
- inference.

FastAPI provides:

- typed APIs;
- Pydantic validation;
- simple health/administrative endpoints;
- good integration with Python tooling.

Using Python also demonstrates a realistic polyglot architecture.

## Alternatives Considered

### Implement fraud logic inside Spring Boot

Simpler operationally, but provides weaker separation between transaction processing and data/ML concerns.

### Separate Java fraud service

Would preserve one language but adds little value for the ML-focused component.

## Consequences

Positive:

- strong ML ecosystem;
- clear service boundary;
- broader portfolio coverage.

Negative:

- two language ecosystems must be maintained;
- separate tooling, packaging, and CI are required;
- event contracts must remain language-neutral.

## Constraints

- Kafka event schemas must not depend on Java or Python-specific serialization.
- Business contracts must be documented independently.
- The service must pass Ruff, MyPy, and Pytest checks.
- ML inference must be behind an explicit interface so rule-based and ML implementations can coexist.
