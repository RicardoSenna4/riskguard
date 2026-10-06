-- Índices alinhados às consultas de dashboard e filtros ordenados por data.
CREATE INDEX IF NOT EXISTS ix_transactions_status_created_id ON transactions(status, created_at DESC, id);
CREATE INDEX IF NOT EXISTS ix_transactions_customer_status_created ON transactions(customer_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_transactions_customer_risk_created ON transactions(customer_id, risk, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_transactions_merchant_created ON transactions(merchant, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_fraud_analyses_decision_created ON fraud_analyses(decision, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_audit_resource_created ON audit_events(resource_type, resource_id, created_at DESC);
