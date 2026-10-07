# ADR-007: Fraud Service persiste em PostgreSQL (database-per-service)

- Status: Aceito
- Data: 2026-10-06

## Contexto

O Docker Compose configurava o Fraud Service com `sqlite:////tmp/riskguard-fraud.db`, fora de qualquer volume. Isso divergia da arquitetura aprovada (PostgreSQL como sistema de registro), dificultava concorrência entre API e consumidor Kafka e fazia as análises de fraude desaparecerem a cada recriação do container, o que é inaceitável para um domínio de fraude e auditoria.

## Decisão

- O Fraud Service usa um banco próprio, `riskguard_fraud`, na mesma instância PostgreSQL do Compose. O Transaction Service continua em `riskguard`.
- O banco é criado por `infrastructure/database/init/01-create-fraud-database.sh`, idempotente: roda automaticamente em volumes novos e é reexecutado por `scripts/dev-up.sh` para volumes já existentes.
- O schema é versionado por Alembic. A API aplica `alembic upgrade head` antes de subir.
- Driver: `psycopg` 3 (`postgresql+psycopg://`).

## Consequências

- Análises sobrevivem a `docker compose down` e à recriação dos containers (volume `postgres_data`).
- Cada serviço é dono do próprio banco; não há joins nem acesso cruzado entre `riskguard` e `riskguard_fraud`. A comunicação continua apenas por eventos Kafka.
- Uma instância compartilhada é uma simplificação de ambiente local; em produção os bancos podem ficar em instâncias separadas sem mudança de código, só de `DATABASE_URL`.
- O CI executa as migrações do Fraud Service contra PostgreSQL real.
