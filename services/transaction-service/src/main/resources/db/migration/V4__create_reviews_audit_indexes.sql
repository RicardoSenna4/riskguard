CREATE TABLE fraud_reviews (id UUID PRIMARY KEY, transaction_id UUID NOT NULL REFERENCES transactions(id) ON DELETE CASCADE, reviewer_id UUID NOT NULL REFERENCES users(id), decision VARCHAR(20) NOT NULL, reason VARCHAR(1000) NOT NULL, automatic_decision VARCHAR(20) NOT NULL, original_risk_score NUMERIC(5,4) NOT NULL, correlation_id VARCHAR(100) NOT NULL, reviewed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, CONSTRAINT ck_review_decision CHECK (decision IN ('APPROVE','BLOCK')));
CREATE INDEX ix_fraud_reviews_transaction ON fraud_reviews(transaction_id, reviewed_at);
CREATE TABLE audit_events (id UUID PRIMARY KEY, action VARCHAR(80) NOT NULL, actor_id UUID REFERENCES users(id), correlation_id VARCHAR(100) NOT NULL, resource_type VARCHAR(80), resource_id UUID, before_json TEXT, after_json TEXT, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP);
CREATE INDEX ix_audit_created_at ON audit_events(created_at DESC);
CREATE INDEX ix_transactions_status_created ON transactions(status, created_at DESC);
CREATE INDEX ix_transactions_risk_score ON transactions(risk_score);
