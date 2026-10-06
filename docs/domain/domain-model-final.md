# Modelo de domínio final

`UserAccount` possui papel USER, ANALYST ou ADMIN; `Customer` pertence a um usuário; `Transaction` referencia customer, valor/moeda/merchant, status, risco, optimistic locking e timestamps. `TransactionStatusHistory` mantém o histórico append-only.

`FraudAnalysis` guarda score entre 0 e 1, decisão automática, reasons, versão do modelo, evento e correlation ID. `FraudReview` guarda decisão manual, nota, decisão automática e score original, preservando a explicabilidade. `AuditEvent` registra ator, ação, recurso, before/after, timestamp e correlation ID.

`IdempotencyKey` impede efeitos duplicados de requisições HTTP. `OutboxEvent` registra a intenção de publicação no mesmo commit da transação. `ProcessedEvent` evita reaplicação de mensagens. PostgreSQL é a fonte de verdade e as migrações Flyway são imutáveis.
