# Validação final

## Qualidade

Java usa `mvn test`/`mvn verify`; Python usa Ruff, MyPy e Pytest; frontend usa ESLint, Vitest e build TypeScript/Vite. Testcontainers oferece uma verificação PostgreSQL opcional com `RUN_CONTAINERS=true`.

## Observabilidade

Consulte `/actuator/health`, `/actuator/prometheus` e `/metrics`. Grafana consolida latência, erros, throughput, fraude e Outbox. Kafka UI permite inspecionar tópicos e DLQ.

## Troubleshooting

Se a API não inicia, verifique `JWT_SECRET`, health do PostgreSQL/Redis/Kafka e logs do serviço. Se o dashboard estiver vazio, execute `./scripts/seed-demo.sh`. Se houver Outbox pendente, verifique Kafka e o contador de tentativas. Se a análise não avançar, consulte `fraud.analysis.dlq.v1` e o correlation ID. Se o frontend retornar 502, aguarde o healthcheck do Transaction Service e confirme o proxy Nginx.
