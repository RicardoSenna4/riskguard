# ADR-006: Compose local com PostgreSQL, Redis e Kafka em KRaft

- Status: Aceito
- Data: 2026-09-29

## Contexto e decisão

O projeto precisa de dependências locais reproduzíveis sem cluster Zookeeper separado. Um Docker Compose oferece PostgreSQL como sistema de registro, Redis para uso temporário futuro, Kafka em modo combinado KRaft para desenvolvimento e Kafka UI. Cada serviço tem healthcheck; dados locais usam volumes nomeados.

## Consequências e limites

- Topologia Kafka de nó único serve somente desenvolvimento e não representa alta disponibilidade.
- Portas e credenciais são configuráveis via `.env`; não expor portas/segredos em redes públicas.
- `dev-down` preserva volumes; limpeza de dados é operação manual separada.
- Imagens têm versões explícitas para repetibilidade; revisão/atualização de versões é uma tarefa de manutenção.
