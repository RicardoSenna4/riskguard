#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ENV_FILE:-$ROOT_DIR/.env}"
[[ -f "$ENV_FILE" ]] || ENV_FILE="$ROOT_DIR/.env.example"
set -a; . "$ENV_FILE"; set +a
COMPOSE_FILE="$ROOT_DIR/infrastructure/docker/docker-compose.yml"
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" exec -T postgres psql -U "${POSTGRES_USER:-riskguard}" -d "${POSTGRES_DB:-riskguard}" < "$ROOT_DIR/infrastructure/seeds/demo.sql"
printf 'Seeds de demonstração aplicados. Senha de todas as contas: correct-horse-battery\n'
