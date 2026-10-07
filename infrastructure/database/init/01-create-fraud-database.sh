#!/bin/sh
# Creates the Fraud Service database (database-per-service on the shared PostgreSQL).
# Runs automatically on a fresh postgres volume via /docker-entrypoint-initdb.d and is
# idempotent, so scripts/dev-up.sh also runs it against existing volumes.
set -eu
: "${FRAUD_DB_NAME:=riskguard_fraud}"
psql -v ON_ERROR_STOP=1 -v fraud_db="$FRAUD_DB_NAME" --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<'SQL'
SELECT format('CREATE DATABASE %I', :'fraud_db')
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = :'fraud_db')\gexec
SQL
