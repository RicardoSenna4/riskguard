# Estratégia de testes

## Implementado

`AuthIntegrationTest` sobe contexto Spring com perfil H2 isolado (sem Docker) e cobre cadastro USER, normalização de email, login, refresh, duplicidade, credenciais inválidas, resposta 401 sem JWT e 403 para USER em endpoint ANALYST.

## Comandos

```bash
cd services/transaction-service
mvn test
mvn verify
```

## Lacunas a cobrir

- ownership: dono, outro USER e ADMIN;
- logout e rejeição do refresh revogado/reutilizado/expirado;
- tokens com assinatura incorreta e expiração;
- integração real Postgres/Flyway via Testcontainers ou ambiente Compose;
- concorrência em refresh simultâneo e retenção/limpeza de tokens expirados;
- endpoints negativos e análise de dependências.
