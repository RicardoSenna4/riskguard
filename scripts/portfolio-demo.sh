#!/usr/bin/env bash
set -euo pipefail
API_URL="${API_URL:-http://localhost:8080}"
FRAUD_URL="${FRAUD_URL:-http://localhost:8090}"
PROM_URL="${PROM_URL:-http://localhost:9090}"
GRAFANA_URL="${GRAFANA_URL:-http://localhost:3000}"
check(){ printf '%-14s' "$1"; curl --fail --silent --show-error --output /dev/null "$2" && echo OK; }
check api-health "$API_URL/actuator/health"
check fraud-health "$FRAUD_URL/health"
check prometheus "$PROM_URL/-/healthy"
check grafana "$GRAFANA_URL/api/health"
check swagger "$API_URL/swagger-ui.html"
printf '\nDemo pronta. Consulte docs/demo/portfolio-demo.md para o roteiro completo.\n'
