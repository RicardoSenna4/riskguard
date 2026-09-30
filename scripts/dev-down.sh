#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$ROOT_DIR/.env"
COMPOSE_FILE="$ROOT_DIR/infrastructure/docker/docker-compose.yml"
[[ -f "$ENV_FILE" ]] || ENV_FILE="$ROOT_DIR/.env.example"
# `down` intentionally keeps named volumes; remove data only with an explicit manual command.
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" down
