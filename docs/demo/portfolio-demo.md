# Demo de portfólio

## Preparação

```bash
cp .env.example .env
# substitua JWT_SECRET por: openssl rand -base64 32
./scripts/dev-up.sh
./scripts/seed-demo.sh
```

Credenciais fictícias dos seeds: `user@riskguard.local`, `analyst@riskguard.local` e `admin@riskguard.local`. A senha é `correct-horse-battery` somente no ambiente local.

## Roteiro

1. Abra `http://localhost:5173` e entre como `analyst@riskguard.local`.
2. Mostre o dashboard com total, aprovadas, revisão, bloqueadas, score médio e merchants.
3. Abra **Transações** e filtre `REVIEW` para mostrar a fila de análise.
4. Abra `DEMO-REVIEW-001` e mostre score, reasons, modelo, timeline e correlation ID.
5. Execute uma revisão manual com motivo; confirme a ação e mostre o histórico/auditoria.
6. Abra `http://localhost:8080/swagger-ui.html`, autorize com o JWT e execute consultas de clientes/transações.
7. Abra Kafka UI para mostrar `transaction.created.v1`, `fraud.analysis.completed.v1` e a DLQ.
8. Abra Grafana para mostrar latência, throughput, fraude e backlog do Outbox.
9. Mostre os testes e o workflow CI no GitHub.

O fluxo é educacional: não há pagamentos, cartão, liquidação ou movimentação financeira real.
