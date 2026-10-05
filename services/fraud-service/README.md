# RiskGuard Fraud Service

Serviço Python/FastAPI responsável por consumir `transaction.created.v1`, validar schema/versionamento, aplicar o `RiskEngine` baseado em regras, persistir a análise e publicar `fraud.analysis.completed.v1`. Eventos inválidos ou que excedem o fluxo de retry são enviados a `fraud.analysis.dlq.v1`.

## Execução

```bash
uv sync --extra dev
uv run uvicorn riskguard_fraud.app:app --app-dir src --port 8090
uv run python -m riskguard_fraud.worker
```

O endpoint `GET /health` é público. O endpoint `POST /api/v1/analyze` é útil para testes e integração local.

## Decisões

- `riskScore` sempre fica entre 0 e 1.
- `APPROVED` abaixo do threshold de revisão, `REVIEW` a partir de 0.45 e `BLOCKED` a partir de 0.80.
- A análise é deduplicada por `transactionId` e o evento consumido por `eventId`.
- SQLAlchemy é usado como fonte persistente do serviço; Kafka é apenas transporte.
