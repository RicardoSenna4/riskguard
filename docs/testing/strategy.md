# Estratégia de testes

## Implementado

`AuthIntegrationTest` sobe contexto Spring com perfil H2 isolado (sem Docker) e cobre cadastro USER, normalização de email, login, refresh, duplicidade, credenciais inválidas, resposta 401 sem JWT e 403 para USER em endpoint ANALYST.

`EventFlowContainersTest` (Testcontainers: PostgreSQL, Kafka, Redis; `RUN_CONTAINERS=true`) usa a configuração de produção, com Flyway, `ddl-auto: validate`, publisher do Outbox e listener Kafka, e cobre o fluxo completo da transação até a atualização de status, incluindo entrega duplicada do resultado.

No Fraud Service, `test_migrations.py` aplica as migrações Alembic (PostgreSQL no CI) e `test_worker.py` verifica o que o worker publica em Kafka. O job `e2e` do CI executa `scripts/e2e-event-flow.sh` contra a stack Compose.

## Comandos

```bash
cd services/transaction-service
mvn test
mvn verify
RUN_CONTAINERS=true mvn verify   # requer Docker
```

## Lacunas a cobrir

- ownership: dono, outro USER e ADMIN;
- logout e rejeição do refresh revogado/reutilizado/expirado;
- tokens com assinatura incorreta e expiração;
- concorrência em refresh simultâneo e retenção/limpeza de tokens expirados;
- endpoints negativos e análise de dependências.
