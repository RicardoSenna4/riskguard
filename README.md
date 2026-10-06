# RiskGuard

Plataforma de portfólio para demonstrar engenharia de dados e software em um fluxo de detecção de fraude: transações persistidas, eventos assíncronos versionados e análise desacoplada. O escopo inicial implementa a fundação do monorepo, infraestrutura local e autenticação/autorização do Transaction Service.

> **Estado:** fundação e serviço transacional inicial. Não processa pagamentos reais nem deve receber dados financeiros reais.

## Arquitetura

```text
Cliente → Transaction Service (Java 21 / Spring Boot)
                       ├── PostgreSQL (fonte de verdade, Flyway)
                       ├── Redis (cache/uso temporário futuro)
                       └── Kafka → Fraud Service (Python/FastAPI, etapa futura)
```

As decisões aprovadas estão em [`docs/decisions/`](docs/decisions/). PostgreSQL permanece como fonte de verdade; Kafka é a fronteira assíncrona; eventos são versionados e consumidores futuros devem ser idempotentes. O monorepo mantém limites explícitos entre componentes.

## Estrutura

```text
services/transaction-service/   API de autenticação e base transacional (Java/Maven)
services/fraud-service/         componente Python reservado para etapa futura
frontend/                       frontend reservado para etapa futura
infrastructure/docker/          Docker Compose local
scripts/                        atalhos dev-up/dev-down
 docs/                           domínio, regras, eventos, segurança, testes e ADRs
```

## Pré-requisitos

- Java 21 e Maven 3.9+
- Docker Engine e Docker Compose v2
- Git

## Inicialização local

```bash
./scripts/dev-up.sh
```

Na primeira execução, o script copia `.env.example` para `.env`; revise os valores de desenvolvimento antes de expor serviços na rede. O Compose inicia PostgreSQL, Redis, Kafka (KRaft) e Kafka UI com healthchecks e volumes persistentes. Os endereços locais padrão são PostgreSQL `localhost:5432`, Redis `localhost:6379`, Kafka `localhost:9092` e Kafka UI `http://localhost:8081`.

Para encerrar os containers **sem apagar dados**:

```bash
./scripts/dev-down.sh
```

As migrações Flyway são executadas quando o Transaction Service inicia. Para executar a API localmente, carregue `.env` no shell (o Docker Compose não exporta variáveis para o processo local) e inicie:

```bash
set -a; . ./.env; set +a
cd services/transaction-service
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

A API fica em `http://localhost:8080/api/v1`; verificação de saúde em `/actuator/health`.

## Autenticação e autorização

- `POST /api/v1/auth/register`: cria usuário com papel `USER`.
- `POST /api/v1/auth/login`: valida senha com BCrypt, retorna JWT de acesso e refresh token opaco.
- `POST /api/v1/auth/refresh`: revoga o refresh token apresentado e emite novo par (rotação de uso único).
- `POST /api/v1/auth/logout`: exige JWT válido e revoga o refresh token do usuário autenticado.
- `GET /api/v1/users/{userId}/profile`: demonstra ownership; o próprio usuário ou `ADMIN` pode consultar.
- `GET /api/v1/analyst/ping` e `/api/v1/admin/ping`: demonstram autorização por papel.

Access token dura 900 s e refresh token 604800 s por padrão; ambos são configuráveis. Novos cadastros sempre recebem `USER`; atribuição de `ANALYST`/`ADMIN` é deliberadamente administrativa e não está exposta por endpoint público. Falta/invalidade de autenticação retorna 401; identidade autenticada sem permissão retorna 403.

Consulte [`docs/api/authentication.md`](docs/api/authentication.md) e [`docs/api/customers-and-transactions.md`](docs/api/customers-and-transactions.md) para contratos e exemplos. O refresh token é devolvido no corpo da resposta neste bootstrap para facilitar o teste; em produção, prefira cookie `HttpOnly`, `Secure`, `SameSite` com política de CSRF apropriada, TLS, rotação de chaves e gestão formal de segredos.

## Configuração

As variáveis documentadas estão em `.env.example`. Nunca versione `.env`, credenciais, tokens ou dados reais. Gere um `JWT_SECRET` Base64 aleatório de pelo menos 256 bits, por exemplo `openssl rand -base64 32`. Os valores de `.env.example` são apenas para desenvolvimento local.

## Verificações

```bash
cd services/transaction-service
mvn test
mvn verify
```

Os testes usam H2 isolado e não exigem containers. Healthchecks e volumes podem ser inspecionados com `docker compose -f infrastructure/docker/docker-compose.yml ps`.

## Documentação

- [`AGENTS.md`](AGENTS.md): regras de contribuição e segurança.
- [`docs/domain/domain-model.md`](docs/domain/domain-model.md): entidades e limites.
- [`docs/domain/business-rules.md`](docs/domain/business-rules.md): regras de negócio atuais.
- [`docs/events/event-catalog.md`](docs/events/event-catalog.md): eventos e envelope versionado.
- [`docs/decisions/`](docs/decisions/): ADRs aceitos.

## Roadmap resumido

- [x] Monorepo, documentação de domínio/regras/eventos e ADRs.
- [x] Infraestrutura local PostgreSQL, Redis, Kafka e Kafka UI.
- [x] Transaction Service: Java 21, Spring Boot, Maven, Actuator, PostgreSQL/Flyway.
- [x] Cadastro, login, hash BCrypt, JWT de acesso, refresh rotativo e logout.
- [x] Papéis USER/ANALYST/ADMIN, endpoints protegidos, ownership e respostas 401/403.
- [x] Gestão de clientes: CRUD, ativação/inativação, documento único, ownership, validação e paginação.
- [x] Gestão de transações: criação, consulta, filtros, paginação, status inicial, validações, cliente ativo, external ID único, optimistic locking e histórico.
- [x] Idempotência HTTP persistida, replay da resposta, conflito por payload diferente, expiração e teste concorrente.
- [x] Tratamento global de erros com códigos internos, correlation ID e sem stack trace exposta.
- [x] Transactional Outbox com atomicidade, retry, tentativas, publicação e métricas de backlog.
- [x] Kafka producer/consumer, eventos versionados, correlation ID, retry, DLQ e deduplicação.
- [x] Fraud Service Python/FastAPI/Pydantic/SQLAlchemy com healthcheck, worker Kafka, retry e DLQ.
- [x] Motor de fraude baseado em regras com score 0–1, motivos e decisões APPROVED/REVIEW/BLOCKED.
- [x] Persistência de FraudAnalysis e atualização assíncrona de Transaction com score, decisão e modelo.
- [x] Publicação/consumo Kafka, Fraud Service, frontend e observabilidade Prometheus/Grafana.
- [x] Segurança com JWT/refresh rotativo, BCrypt, RBAC, ownership, CORS explícito, headers, rate limiting e threat model STRIDE.
- [x] Testes Java unitários/integração, Testcontainers opcional, testes Python, Vitest/Testing Library e smoke E2E Playwright.
- [x] Smoke tests de resiliência/performance, índices de consulta e workflow CI com build, testes, lint, Docker e dependency scan.

## Licença

MIT — consulte [`LICENSE`](LICENSE).
