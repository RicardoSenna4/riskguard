#!/usr/bin/env bash
# End-to-end check of the event-driven flow on the full Docker Compose stack:
#   POST /transactions -> PostgreSQL + outbox -> transaction.created.v1 -> fraud-worker
#   -> riskguard_fraud (PostgreSQL) -> fraud.analysis.completed.v1 -> transaction-service
#   -> status updated. Then recreates the fraud containers and checks the analysis survived.
# Requires a running stack (scripts/dev-up.sh), curl and jq.
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
API_URL="${API_URL:-http://localhost:8080}"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-90}"
compose() { docker compose --env-file "$ROOT_DIR/.env" -f "$ROOT_DIR/infrastructure/docker/docker-compose.yml" "$@"; }
fraud_sql() { compose exec -T postgres sh -c 'psql -tAq -U "$POSTGRES_USER" -d "$FRAUD_DB_NAME" -c "$0"' "$1"; }
fail() {
  printf 'FAIL: %s\n' "$*" >&2
  if [[ -n "${GITHUB_ACTIONS:-}" ]]; then
    local logs; logs="$(compose logs --no-color --tail=15 transaction-service fraud-worker 2>&1 | cut -c1-300)"
    printf '::error title=e2e::%s%%0A%s\n' "$*" "$(sed ':a;N;$!ba;s/%/%25/g;s/\n/%0A/g' <<<"$logs")"
  fi
  compose logs --tail=80 transaction-service fraud-service fraud-worker >&2 || true
  exit 1
}
# Captures the response body so failures can show what the API returned.
api() {
  local out
  if ! out="$(curl --silent --show-error --fail-with-body -H 'Content-Type: application/json' "$@" 2>&1)"; then
    printf '%s -> %s' "${*: -1}" "$out" | tee "$ERR_FILE" >&2; return 1
  fi
  printf '%s' "$out"
}
ERR_FILE="$(mktemp)"; trap 'rm -f "$ERR_FILE"' EXIT
last_error() { cat "$ERR_FILE"; }

run_id="$(date +%s)-$$"
email="e2e-$run_id@example.com"
password='correct-horse-battery'

api -X POST "$API_URL/api/v1/auth/register" -d "{\"email\":\"$email\",\"password\":\"$password\"}" >/dev/null || fail "register: $(last_error)"
token="$(api -X POST "$API_URL/api/v1/auth/login" -d "{\"email\":\"$email\",\"password\":\"$password\"}" | jq -er .accessToken)" || fail "login: $(last_error)"
auth=(-H "Authorization: Bearer $token")

customer_id="$(api "${auth[@]}" -X POST "$API_URL/api/v1/customers" \
  -d "{\"document\":\"E2E-$run_id\",\"name\":\"Cliente E2E\",\"email\":\"customer-$run_id@example.com\"}" | jq -er .id)" || fail "create customer: $(last_error)"

transaction="$(api "${auth[@]}" -H "X-Correlation-Id: e2e-$run_id" -X POST "$API_URL/api/v1/transactions" \
  -d "{\"customerId\":\"$customer_id\",\"externalId\":\"e2e-$run_id\",\"amount\":\"25.00\",\"currency\":\"BRL\",\"merchant\":\"E2E Store\"}")" || fail "create transaction: $(last_error)"
transaction_id="$(jq -er .id <<<"$transaction")"
[[ "$(jq -r .status <<<"$transaction")" == "PENDING" ]] || fail "new transaction should be PENDING: $transaction"
printf 'transaction %s created (PENDING)\n' "$transaction_id"

status=PENDING
deadline=$((SECONDS + TIMEOUT_SECONDS))
while [[ "$status" == "PENDING" && $SECONDS -lt $deadline ]]; do
  sleep 2
  status="$(api "${auth[@]}" "$API_URL/api/v1/transactions/$transaction_id" | jq -r .status)"
done
[[ "$status" =~ ^(APPROVED|REVIEW|BLOCKED)$ ]] || fail "status still $status after ${TIMEOUT_SECONDS}s"
printf 'status updated by fraud analysis: %s\n' "$status"

[[ "$(fraud_sql "SELECT count(*) FROM fraud_analyses WHERE transaction_id = '$transaction_id'")" == "1" ]] \
  || fail "analysis not stored in the fraud database"

compose up -d --force-recreate --wait fraud-service fraud-worker >/dev/null
[[ "$(fraud_sql "SELECT count(*) FROM fraud_analyses WHERE transaction_id = '$transaction_id'")" == "1" ]] \
  || fail "analysis lost after recreating fraud containers"
printf 'analysis persisted in PostgreSQL across container recreation\n'
printf 'E2E event flow OK\n'
