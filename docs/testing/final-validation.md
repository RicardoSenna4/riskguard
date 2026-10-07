# Validação final

## Qualidade

Java usa `mvn test`/`mvn verify`; Python usa Ruff, MyPy e Pytest; frontend usa ESLint, Vitest e build TypeScript/Vite. Com `RUN_CONTAINERS=true` (sempre ativo no CI), `EventFlowContainersTest` sobe PostgreSQL, Kafka e Redis via Testcontainers e valida o fluxo Outbox → Kafka → consumidor com Flyway e validação de schema reais. `scripts/e2e-event-flow.sh` valida o fluxo entre os dois serviços na stack Compose.

## Observabilidade

Consulte `/actuator/health`, `/actuator/prometheus` e `/metrics`. Grafana consolida latência, erros, throughput, fraude e Outbox. Kafka UI permite inspecionar tópicos e DLQ.

## Troubleshooting

Se a API não inicia, verifique `JWT_SECRET`, health do PostgreSQL/Redis/Kafka e logs do serviço. Se o dashboard estiver vazio, execute `./scripts/seed-demo.sh`. Se houver Outbox pendente, verifique Kafka e o contador de tentativas. Se a análise não avançar, confira se o `fraud-worker` está rodando, consulte `fraud.analysis.dlq.v1` e procure no log do Transaction Service por `Sending fraud result to DLQ`, que registra a causa. Se o frontend retornar 502, aguarde o healthcheck do Transaction Service e confirme o proxy Nginx.
