# Eventos de risco e Transactional Outbox

## `transaction.created.v1`

Publicado pelo Transaction Service somente após a transação e o registro do evento serem confirmados no mesmo commit. O payload inclui `eventId`, `eventType`, `schemaVersion`, `correlationId`, `occurredAt`, `transactionId`, `customerId`, `amount`, `currency` e `merchant`.

## `fraud.analysis.completed.v1`

Publicado pelo Fraud Service após validar e persistir a análise. Inclui `eventId`, `eventType`, `schemaVersion`, `correlationId`, `occurredAt`, `transactionId`, `riskScore`, `decision`, `reasons` e `modelVersion`.

## `fraud.analysis.dlq.v1`

Recebe eventos inválidos ou que excederam as tentativas de processamento. O payload preserva o evento original e a mensagem de erro para diagnóstico.

## Garantias

- `outbox_events` registra `PENDING`, tentativas, `nextAttemptAt`, `lastError` e `PUBLISHED`/`publishedAt`.
- O publisher tenta novamente com backoff exponencial limitado a cinco minutos.
- O consumidor Java aplica três tentativas e envia falhas à DLQ.
- O Fraud Service deduplica por `eventId` e `transactionId`.
- O retorno de fraude atualiza score, risco, versão do modelo, status e histórico da transação em uma transação de banco.
- `correlationId` é propagado entre REST, Outbox, Kafka, Fraud Service e resposta de análise.
