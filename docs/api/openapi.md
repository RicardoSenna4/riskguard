# OpenAPI e Swagger

Com o Transaction Service em execução, a especificação está disponível em [`/v3/api-docs`](http://localhost:8080/v3/api-docs) e a UI em [`/swagger-ui.html`](http://localhost:8080/swagger-ui.html). O botão **Authorize** recebe somente o JWT de acesso, sem o prefixo `Bearer` quando a UI já o aplica.

A especificação cobre autenticação, clientes, transações, detalhes, histórico, reviews, dashboard e o esquema de erro padronizado. Operações protegidas usam o esquema `bearerAuth`; cadastro, login e refresh são públicos. Todas as respostas de erro devem conter código interno e correlation ID, sem stack trace.

## Exemplos rápidos

```bash
TOKEN=$(curl -s localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"user@riskguard.local","password":"correct-horse-battery"}' | jq -r .accessToken)

curl -H "Authorization: Bearer $TOKEN" \
  'localhost:8080/api/v1/transactions?page=0&size=20'
```

O contrato JSON de eventos Kafka está em [`docs/events/risk-analysis-events.md`](../events/risk-analysis-events.md). Alterações incompatíveis devem criar uma nova versão (`v2`) em vez de alterar silenciosamente o envelope existente.
