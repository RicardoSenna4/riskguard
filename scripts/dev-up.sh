#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$ROOT_DIR/.env"
COMPOSE_FILE="$ROOT_DIR/infrastructure/docker/docker-compose.yml"
if [[ ! -f "$ENV_FILE" ]]; then
  cp "$ROOT_DIR/.env.example" "$ENV_FILE"
  printf 'Criado .env a partir do exemplo. Revise credenciais locais antes de expor portas.\n'
fi
cd "$ROOT_DIR"
compose() { docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" "$@"; }
# Fresh volumes create the fraud database through docker-entrypoint-initdb.d; volumes created
# before it existed need the same idempotent script, so run it before starting the services.
compose up -d --wait postgres
compose exec -T postgres sh /docker-entrypoint-initdb.d/01-create-fraud-database.sh
compose up -d --wait
