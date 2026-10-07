# RiskGuard Fraud Service

Serviço Python/FastAPI responsável por consumir `transaction.created.v1`, validar schema/versionamento, aplicar o `RiskEngine` baseado em regras, persistir a análise e publicar `fraud.analysis.completed.v1`. Eventos inválidos ou que excedem o fluxo de retry são enviados a `fraud.analysis.dlq.v1`.

## Execução

```bash
uv sync --extra dev
export DATABASE_URL=postgresql+psycopg://riskguard:change-me-local-only@localhost:5432/riskguard_fraud
uv run alembic upgrade head
uv run uvicorn riskguard_fraud.app:app --app-dir src --port 8090
PYTHONPATH=src uv run python -m riskguard_fraud.worker
```

No Docker Compose, a API (`fraud-service`) aplica as migrações Alembic antes de subir e o consumidor Kafka roda como `fraud-worker`, com a mesma imagem. Ambos usam o banco `riskguard_fraud` no PostgreSQL compartilhado (ver [ADR-007](../../docs/decisions/ADR-007-fraud-service-postgresql.md)). Sem `DATABASE_URL`, o serviço cai para um SQLite local, útil apenas para experimentos rápidos.

O endpoint `GET /health` é público. O endpoint `POST /api/v1/analyze` é útil para testes e integração local.

## Decisões

- `riskScore` sempre fica entre 0 e 1.
- `APPROVED` abaixo do threshold de revisão, `REVIEW` a partir de 0.45 e `BLOCKED` a partir de 0.80.
- A análise é deduplicada por `transactionId` e o evento consumido por `eventId`.
- PostgreSQL (via SQLAlchemy + Alembic) é a fonte persistente do serviço; Kafka é apenas transporte.
