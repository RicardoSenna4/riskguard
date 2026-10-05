# Clientes, transações e idempotência (API v1)

Todos os endpoints abaixo exigem `Authorization: Bearer <accessToken>`. Usuários `USER` acessam apenas seus próprios clientes e transações; `ANALYST` e `ADMIN` têm acesso operacional global.

## Clientes

- `POST /api/v1/customers` — cria cliente. Campos obrigatórios: `document`, `name`, `email`. O documento é único globalmente. Aceita `Idempotency-Key`.
- `GET /api/v1/customers/{id}` — consulta cliente.
- `GET /api/v1/customers?page=0&size=20` — lista paginada; `size` máximo 100.
- `PUT /api/v1/customers/{id}` — atualiza `name` e `email`.
- `POST /api/v1/customers/{id}/activate` e `/inactivate` — altera o estado ativo.

Um cliente inativo não pode originar novas transações.

## Transações

- `POST /api/v1/transactions` — cria transação com status inicial `PENDING` e risco inicial `UNKNOWN`. Campos: `customerId`, `externalId`, `amount`, `currency` (ISO-4217 de três letras) e `merchant`. Aceita `Idempotency-Key`.
- `GET /api/v1/transactions/{id}` — consulta transação.
- `GET /api/v1/transactions` — lista paginada com filtros opcionais `customerId`, `from`, `to`, `status`, `risk`, `merchant`, `page` e `size`.
- `PATCH /api/v1/transactions/{id}/status` — altera status com `{ "status": "APPROVED", "version": 0 }`. A versão precisa ser a última conhecida; conflito retorna `409`.
- `GET /api/v1/transactions/{id}/status-history` — consulta o histórico, incluindo a entrada `PENDING` inicial.

Valores monetários usam `NUMERIC(19,4)`/`BigDecimal`; valores menores ou iguais a zero são rejeitados. `externalId` é único globalmente.

## Idempotência HTTP

Para operações de criação, envie `Idempotency-Key`. A chave é persistida no PostgreSQL por usuário durante 24 horas (configurável por `IDEMPOTENCY_EXPIRATION_SECONDS`). A mesma chave e o mesmo payload retornam a resposta original; a mesma chave com payload diferente retorna `409 CONFLICT`. Requisições concorrentes na mesma chave são serializadas e retornam o mesmo recurso.

## Erros

O formato é estável e não expõe stack trace:

```json
{
  "timestamp": "2026-10-05T00:00:00Z",
  "status": 409,
  "error": "Conflict",
  "code": "CONFLICT",
  "message": "...",
  "path": "/api/v1/transactions",
  "correlationId": "..."
}
```

São cobertos `400`, `401`, `403`, `404`, `409`, `429` e `500`. O `X-Correlation-Id` recebido é preservado; quando ausente, a API gera um novo e o devolve no header e no corpo do erro.
