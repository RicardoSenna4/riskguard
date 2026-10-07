# RiskGuard V1

## Escopo entregue

A V1 demonstra o fluxo ponta a ponta: usuário → React/Nginx → Transaction Service → PostgreSQL/Outbox → Kafka → Fraud Service → decisão → Kafka → atualização da transação → dashboard/review. Inclui autenticação JWT, refresh rotativo, BCrypt, RBAC, ownership, idempotência, auditoria, rate limiting, correlação distribuída, regras de fraude, integração ML, Prometheus/Grafana, OpenAPI, Docker Compose, seeds, testes e CI.

## Critérios de aceite

- `./scripts/dev-up.sh` sobe a infraestrutura local após `.env` válido.
- `./scripts/seed-demo.sh` é idempotente e povoa a demonstração.
- `mvn verify` (com Testcontainers), Ruff/MyPy/Pytest, ESLint/Vitest/build frontend passam.
- As três imagens Docker são construídas no CI e o E2E (`scripts/e2e-event-flow.sh`) confirma o fluxo de eventos entre os serviços.
- Trivy não reporta CRITICAL/HIGH e o CodeQL roda a cada push.
- Swagger, health, métricas, Kafka UI e Grafana estão acessíveis.
- Duplicidade de HTTP/eventos não cria efeitos duplicados.
- Falhas de Kafka/Fraud Service preservam o Outbox e encaminham falhas permanentes à DLQ.
- Logs e respostas carregam correlation ID sem expor secrets ou stack traces.

## Fora do escopo

A V1 não processa dinheiro real, não oferece PCI scope, MFA, alta disponibilidade, Kafka TLS/ACL de produção, autoscaling, SLOs de produção ou modelo ML validado com dados reais. O dataset ML é externo, sintético/limitado e não deve ser usado como decisão financeira sem validação, monitoramento de drift e revisão humana.

## Próximos passos

Configurar secret manager e TLS, adicionar DAST contínuo, executar carga representativa, integrar tracing OpenTelemetry, revisar acessibilidade visual e publicar imagens versionadas em registry.
