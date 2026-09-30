# Catálogo de eventos

Ainda não há produtor/consumidor de domínio ativo nesta fundação. Os contratos abaixo são planejados conforme ADR-002/003; qualquer mudança deve ser compatível ou receber nova versão e revisão.

| Tópico | Produtor → consumidor | Estado |
|---|---|---|
| `riskguard.transaction.created.v1` | Transaction Service → Fraud Service | Planejado |
| `riskguard.fraud.analysis.completed.v1` | Fraud Service → Transaction Service | Planejado |
| `riskguard.fraud.analysis.dlq.v1` | Fraud Service → operação/reprocessamento | Planejado |

Envelope JSON comum:

```json
{
  "eventId": "UUID",
  "eventType": "transaction.created",
  "eventVersion": 1,
  "occurredAt": "2026-09-29T12:00:00Z",
  "correlationId": "UUID",
  "data": {}
}
```

Requisitos antes de ativar: persistir intenção e entidade no mesmo commit via Outbox; entrega Kafka é pelo menos uma vez; consumidores deduplicam por `eventId`; retry limitado e DLQ para falha permanente. PII e credenciais não podem ser incluídas no payload. Campos de valor futuro usam decimal string/NUMERIC com moeda explícita, sem float.
