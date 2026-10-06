#!/usr/bin/env bash
set -euo pipefail
URL="${URL:-http://localhost:8080/actuator/health}"
REQUESTS="${REQUESTS:-100}"
TMP="$(mktemp)"; trap 'rm -f "$TMP"' EXIT
for _ in $(seq 1 "$REQUESTS"); do curl --silent --output /dev/null --write-out '%{http_code} %{time_total}\n' "$URL" >>"$TMP"; done
awk '{code[$1]++; times[++n]=$2} END { if(n==0) exit 1; for(i=1;i<=n;i++) for(j=i+1;j<=n;j++) if(times[j]<times[i]){x=times[i];times[i]=times[j];times[j]=x} printf("requests=%d errors=%d p50=%.4fs p95=%.4fs p99=%.4fs\n",n,n-code["200"],times[int(n*.50)+1],times[int(n*.95)+1],times[int(n*.99)+1]) }' "$TMP"
