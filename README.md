# RiskGuard

RiskGuard é uma plataforma educacional de detecção de fraude para demonstrar uma arquitetura distribuída ponta a ponta: frontend React, API transacional Java, PostgreSQL, Redis, Kafka e serviço de fraude Python com regras e ML. **Não processa pagamentos nem dados financeiros reais.**

## Arquitetura

```text
React/Nginx → Transaction Service (Java 21/Spring Boot)
                    ├─ PostgreSQL + Flyway (source of truth)
                    ├─ Redis (cache/rate limit temporário)
                    └─ Transactional Outbox → Kafka
                                                └→ Fraud Service (Python/FastAPI)
                                                    └→ decisão → Kafka → Transaction Service
```

O fluxo é assíncrono e idempotente. Transação e Outbox são gravados no mesmo commit; consumidores deduplicam eventos; `correlationId` atravessa REST, logs, Outbox, Kafka e respostas. As decisões automáticas podem ser `APPROVED`, `REVIEW` ou `BLOCKED`; reviews humanas preservam score, decisão automática, motivo e auditoria.

## Stack

- **Backend:** Java 21, Spring Boot, Spring Security, JWT, BCrypt, JPA, Flyway, Actuator, Micrometer.
- **Fraude:** Python 3.12, FastAPI, Pydantic, SQLAlchemy, Alembic, Kafka, regras, Logistic Regression/Random Forest.
- **Frontend:** React, TypeScript, Vite, React Router, TanStack Query, React Hook Form, Zod, Vitest, Playwright.
- **Infra:** PostgreSQL, Redis, Kafka KRaft, Kafka UI, Prometheus, Grafana, Docker Compose.

## Execução local

```bash
cp .env.example .env
# gere um segredo real para desenvolvimento local:
openssl rand -base64 32
# coloque o valor em JWT_SECRET no .env
./scripts/dev-up.sh
./scripts/seed-demo.sh
./scripts/portfolio-demo.sh
```

Interfaces: frontend `http://localhost:5173`, API `http://localhost:8080`, Fraud Service `http://localhost:8090`, Kafka UI `http://localhost:8081`, Prometheus `http://localhost:9090`, Grafana `http://localhost:3000` e Swagger `http://localhost:8080/swagger-ui.html`.

Para desligar sem remover volumes:

```bash
./scripts/dev-down.sh
```

Seeds locais: `user@riskguard.local`, `analyst@riskguard.local` e `admin@riskguard.local`, todos com senha `correct-horse-battery`. Nunca use essas credenciais fora do ambiente local.

## API e eventos

A documentação OpenAPI fica em [`docs/api/openapi.md`](docs/api/openapi.md). Os endpoints cobrem autenticação, clientes, transações paginadas/filtros, detalhes, histórico, dashboard e reviews. O catálogo de eventos está em [`docs/events/event-catalog.md`](docs/events/event-catalog.md), com `transaction.created.v1`, `fraud.analysis.completed.v1` e DLQ.

## Machine Learning

O Fraud Service inicia com regras por padrão (`RISK_ENGINE=rules`). O pipeline ML seleciona dataset público, documenta limitações, prepara features, treina Logistic Regression/Random Forest e gera métricas. Artefatos grandes e datasets não são versionados; consulte [`services/fraud-service/ml/README.md`](services/fraud-service/ml/README.md).

## Segurança e observabilidade

JWT curto e refresh rotativo usam BCrypt e hash SHA-256; RBAC e ownership protegem endpoints; rate limiting usa Redis com fallback; CORS aceita origens explícitas; respostas não expõem stack trace. Consulte [`docs/security/threat-model.md`](docs/security/threat-model.md) e [`docs/security/release-checklist.md`](docs/security/release-checklist.md).

Actuator, Prometheus e Grafana expõem health, latência, throughput, erros, fraude e backlog do Outbox. Kafka UI permite inspeção operacional e troubleshooting.

## Testes e CI

```bash
cd services/transaction-service && mvn verify
cd ../fraud-service && uv sync --extra dev && uv run ruff check src tests ml && uv run mypy src && uv run pytest
cd ../../frontend && npm ci && npm run lint && npm test -- --run && npm run build
```

O workflow [`CI`](.github/workflows/ci.yml) executa:

| Job | O que valida |
| --- | --- |
| `java` | `mvn verify`, incluindo Testcontainers (PostgreSQL, Kafka e Redis reais): `POST /transactions` → PostgreSQL → Outbox → `transaction.created.v1` → `fraud.analysis.completed.v1` → status atualizado |
| `python` | Ruff, MyPy, Pytest; migrações Alembic contra PostgreSQL |
| `frontend` | ESLint, Vitest, build |
| `infra` | `docker compose config` e build das três imagens (transaction, fraud, frontend) |
| `e2e` | Stack Compose completa com [`scripts/e2e-event-flow.sh`](scripts/e2e-event-flow.sh): fluxo entre os dois serviços e persistência das análises após recriar os containers |
| `dependency-scan` | Trivy (CRITICAL/HIGH) e Dependency Review em PRs |

O workflow [`CodeQL`](.github/workflows/codeql.yml) faz análise estática de Java, Python e TypeScript. Falhas de teste, achados do Trivy e serviços não saudáveis do E2E aparecem como anotações no resumo do run.

Localmente, os testes Testcontainers rodam com `RUN_CONTAINERS=true mvn verify` e o E2E com `./scripts/dev-up.sh && ./scripts/e2e-event-flow.sh`.

Smoke tests:

```bash
./scripts/resilience-smoke.sh
./scripts/performance-smoke.sh
```

## Documentação e diagramas

- [`docs/architecture/architecture.md`](docs/architecture/architecture.md)
- [`docs/domain/domain-model-final.md`](docs/domain/domain-model-final.md)
- [`docs/diagrams/`](docs/diagrams/)
- [`docs/demo/portfolio-demo.md`](docs/demo/portfolio-demo.md)
- [`docs/release-v1.md`](docs/release-v1.md)
- [`docs/testing/final-validation.md`](docs/testing/final-validation.md)
- [`docs/decisions/`](docs/decisions/)

## Limitações

O projeto é uma demonstração de engenharia. Kafka local usa configuração de desenvolvimento; produção exige TLS/ACL, secret manager, tracing, backups, alta disponibilidade, SLOs, validação do modelo, monitoramento de drift e revisão de compliance. Nenhuma decisão deve autorizar dinheiro real sem controles adicionais.

## Licença

MIT — consulte [`LICENSE`](LICENSE).
