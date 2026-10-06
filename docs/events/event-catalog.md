# Catálogo de eventos

| Tópico | Produtor → consumidor | Entrega | Deduplicação |
|---|---|---|---|
| `transaction.created.v1` | Transaction Service → Fraud Service | Outbox + retry | `eventId`/`transactionId` |
| `fraud.analysis.completed.v1` | Fraud Service → Transaction Service | retry limitado | `eventId`/análise |
| `fraud.analysis.dlq.v1` | qualquer consumidor → operação | diagnóstico/reprocessamento | `eventId` |

Envelope JSON comum: `eventId`, `eventType`, `schemaVersion`, `correlationId`, `occurredAt` e dados específicos. O evento de criação contém transaction/customer/amount/currency/merchant; o resultado contém transaction/riskScore/decision/reasons/modelVersion.

Eventos são compatíveis dentro da mesma versão. Campos obrigatórios não são removidos; mudanças incompatíveis criam `v2`. Segredos, tokens e dados de cartão nunca são publicados. O correlation ID é preservado nos headers Kafka e no payload. PostgreSQL é a fonte de verdade; Kafka oferece entrega pelo menos uma vez e pode duplicar mensagens.
