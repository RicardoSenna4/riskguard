# Resiliência e performance

## Resiliência

`resilience-smoke.sh` verifica health e recuperação com retry. Para um cenário local completo, pare individualmente Fraud Service, Kafka e Redis; crie uma transação enquanto o componente estiver indisponível; restaure os containers e verifique que o Outbox, o retry/DLQ e a deduplicação não perderam o evento. PostgreSQL permanece como source of truth. Eventos repetidos são deduplicados por `eventId`/`transactionId`.

O aceite mínimo é: nenhuma transação criada desaparece; eventos não publicáveis continuam PENDING com tentativas; falhas de schema vão para DLQ; Redis indisponível não impede a operação principal; consumidor reiniciado retoma do offset.

## Performance

`performance-smoke.sh` executa carga HTTP controlada e imprime taxa de erro e p50/p95/p99. Não é substituto de teste de carga distribuído. Antes de comparar execuções, fixe quantidade de requests, payload, ambiente, JVM e banco. Para investigação, use `EXPLAIN (ANALYZE, BUFFERS)` nas consultas do dashboard e transações, confirme ausência de N+1 e observe Hikari/Actuator.

A migração `V5__performance_indexes.sql` cobre filtros por status, customer, risco, merchant e ordenação por data. Metas iniciais de laboratório: p95 de leitura abaixo de 300 ms e p99 abaixo de 800 ms com 100 req/s, sem erros; metas de produção devem ser recalibradas com dados reais.
