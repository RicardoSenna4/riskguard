# Modelo de domínio inicial

## Limites

A aplicação inicia com identidade e sessões de autenticação para a futura plataforma RiskGuard. Nenhuma entidade representa dinheiro movimentado: a entidade `Transaction` é futura e não deve ser presumida no modelo atual.

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

## Entidades planejadas

`Customer`, `Transaction`, `FraudAnalysis`, `FraudReview`, `OutboxEvent`. O desenho transacional e de risco será registrado antes de implementação; valores monetários serão `NUMERIC`/`BigDecimal` e datas serão UTC.
