#!/usr/bin/env bash
set -euo pipefail
BASE_URL="${BASE_URL:-http://localhost:8080}"
ATTEMPTS="${ATTEMPTS:-5}"

echo "[1/3] health endpoint"
curl --fail --silent --show-error "$BASE_URL/actuator/health" >/dev/null

echo "[2/3] retry com backoff curto"
for attempt in $(seq 1 "$ATTEMPTS"); do
  if curl --fail --silent --show-error "$BASE_URL/actuator/health" >/dev/null; then break; fi
  sleep "$attempt"
  [[ "$attempt" == "$ATTEMPTS" ]] && { echo "health never recovered" >&2; exit 1; }
done

echo "[3/3] completed"
echo "Para testar falhas reais: docker compose stop fraud-service kafka redis; execute este script; restaure com docker compose start." 
