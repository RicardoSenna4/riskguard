# Threat model — RiskGuard

Escopo: React → Transaction Service → PostgreSQL/Redis/Kafka → Fraud Service → Kafka de retorno. PostgreSQL é a fonte de verdade; Redis e Kafka não substituem persistência.

| Categoria STRIDE | Cenário | Controle atual/implementado | Risco residual |
|---|---|---|---|
| Spoofing | roubo de credencial, refresh token ou JWT | BCrypt, JWT curto, refresh rotativo/revogado, respostas genéricas, rate limiting | MFA e lockout ainda são recomendados |
| Tampering | alteração de transação/evento | validação de entrada, optimistic locking, Outbox, TLS/ACL Kafka no ambiente real | Kafka local usa PLAINTEXT e deve ser substituído |
| Repudiation | usuário negar ação | AuditEvent com actor, timestamp, correlation ID e before/after; logs estruturados | retenção/imutabilidade de logs precisa de SIEM |
| Information Disclosure | stack trace, segredo ou dado sensível em resposta/log | handler global sem stack trace, headers de segurança, segredos via ambiente, tokens não logados | revisão periódica de dependências e PII |
| Denial of Service | brute force, flooding HTTP/Kafka ou payload excessivo | rate limiting Redis com fallback seguro, limites de paginação, retry limitado, DLQ | WAF, quotas Kafka e autoscaling em produção |
| Elevation of Privilege | USER acessa review/admin/ownership alheio | RBAC method security, ownership por customer/user, endpoints administrativos protegidos | revisão formal de matriz de autorização |

## Assumptions e decisões

- Dados locais são fictícios e nunca devem conter PAN, CVV ou credenciais reais.
- `JWT_SECRET`, senhas e credenciais de infraestrutura são injetados pelo ambiente/secret manager.
- CORS com credenciais não aceita wildcard; origens devem ser explícitas.
- Em produção, Kafka, PostgreSQL, Redis e Actuator devem ter rede privada, autenticação, TLS e allowlist.

## Verificações

```bash
mvn test
uv run ruff check src tests ml && uv run mypy src && uv run pytest
npm run build && npm test -- --run
```

O threat model deve ser revisitado quando forem adicionados pagamentos reais, novos papéis, integrações externas ou mudança no fluxo de tokens.
