# Arquitetura técnica

O RiskGuard é um monorepo com três componentes: frontend React servido por Nginx, Transaction Service em Java/Spring Boot e Fraud Service em Python/FastAPI. PostgreSQL é a fonte de verdade; Redis atende cache/rate limit temporários; Kafka transporta eventos com entrega pelo menos uma vez.

## Fluxos

A criação de transação grava `transactions` e `outbox_events` no mesmo commit. O publisher publica `transaction.created.v1`; o Fraud Service valida, deduplica, aplica regras ou ML, persiste `FraudAnalysis` e publica `fraud.analysis.completed.v1`. O Transaction Service atualiza score/status/histórico. Falhas permanentes seguem para DLQ.

O frontend usa o mesmo host do Nginx; `/api/` é encaminhado ao Transaction Service. O correlation ID nasce no REST quando ausente e acompanha MDC, Outbox, headers Kafka, eventos, logs e respostas HTTP.

## Fonte de verdade e consistência

PostgreSQL mantém usuários, clientes, transações, análises, reviews, auditoria, idempotência e Outbox. Redis nunca é requisito para persistência. Kafka pode duplicar mensagens; consumidores deduplicam por `eventId`/`transactionId`. A consistência eventual entre transação e decisão de risco é explícita na UX.

## Operação local

```bash
cp .env.example .env
# substitua JWT_SECRET e credenciais locais
./scripts/dev-up.sh
./scripts/seed-demo.sh
```

Interfaces: frontend `http://localhost:5173`, API `http://localhost:8080`, Fraud Service `http://localhost:8090`, Kafka UI `http://localhost:8081`, Prometheus `http://localhost:9090`, Grafana `http://localhost:3000` e Swagger `http://localhost:8080/swagger-ui.html`.
