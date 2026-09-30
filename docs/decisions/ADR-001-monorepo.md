# ADR-001: Use a Monorepo

- Status: Accepted
- Date: 2026-09-29

## Context

RiskGuard contains multiple components:

- Transaction Service;
- Fraud Service;
- Frontend;
- infrastructure configuration;
- documentation;
- CI workflows.

The project is maintained primarily as a portfolio and learning project by a small development team.

## Decision

Use a single Git repository containing all RiskGuard components.

## Rationale

A monorepo provides:

- simpler local setup;
- one source of architectural documentation;
- easier coordinated changes;
- simpler CI management;
- easier review by recruiters and interviewers;
- easier versioning during the early project stages.

## Alternatives Considered

### Separate repository per service

Advantages:

- stronger service isolation;
- independent versioning.

Disadvantages:

- more repository management;
- duplicated configuration;
- more complex local development;
- unnecessary operational overhead for the current project scale.

## Consequences

Positive:

- easier development and documentation;
- simpler portfolio presentation;
- centralized CI.

Negative:

- repository size can grow;
- CI must avoid rebuilding unrelated components unnecessarily.

## Constraints

The monorepo decision does not mean components may be tightly coupled.

Services must retain explicit API/event boundaries.
