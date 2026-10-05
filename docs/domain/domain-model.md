# Modelo de domínio inicial

## Limites

A aplicação mantém identidade, clientes e transações educacionais para a plataforma RiskGuard. O serviço não autoriza movimentação de dinheiro real.

## Entidades implementadas

### UserAccount (`users`)

- `id`: UUID primário.
- `email`: único, normalizado para minúsculas.
- `passwordHash`: BCrypt.
- `role`: `USER`, `ANALYST` ou `ADMIN`.
- `createdAt`: instante UTC.

O cadastro público só pode criar `USER`. Papéis elevados são atribuídos fora dos endpoints públicos.

### RefreshToken (`refresh_tokens`)

- `id`: UUID; `userId`: FK para `users`.
- `tokenHash`: SHA-256 do valor aleatório entregue ao cliente; único.
- `expiresAt`, `revokedAt`, `createdAt`: instantes UTC.

Tokens são de uso único durante a rotação. PostgreSQL mantém a fonte de verdade das sessões; Redis não é utilizado para persistência ou autorização.

### Customer (`customers`)

- `id`: UUID; `userId`: proprietário autenticado.
- `document`: único globalmente.
- `name`, `email`: campos validados.
- `active`: controla se novas transações podem ser criadas.
- `version`, `createdAt`, `updatedAt`: optimistic locking e auditoria temporal.

### Transaction (`transactions`)

- `id`: UUID; `customerId`: cliente associado.
- `externalId`: único globalmente.
- `amount`: `NUMERIC(19,4)`/`BigDecimal`, sempre positivo.
- `currency`: código de três letras normalizado para maiúsculas.
- `status`: inicia em `PENDING`; `risk`: inicia em `UNKNOWN`.
- `version`, `createdAt`, `updatedAt`: optimistic locking e ordenação.

### TransactionStatusHistory (`transaction_status_history`)

Registra o status inicial e cada alteração com instante UTC e usuário responsável.

### IdempotencyKey (`idempotency_keys`)

Persiste a chave por usuário, hash do payload, resposta e expiração. PostgreSQL é a fonte de verdade; a constraint `(userId, keyValue)` impede criação duplicada.

## Entidades planejadas

`Customer`, `Transaction`, `FraudAnalysis`, `FraudReview`, `OutboxEvent`. O desenho transacional e de risco será registrado antes de implementação; valores monetários serão `NUMERIC`/`BigDecimal` e datas serão UTC.
