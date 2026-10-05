CREATE TABLE customers (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    document VARCHAR(40) NOT NULL,
    name VARCHAR(160) NOT NULL,
    email VARCHAR(320) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_customers_document UNIQUE (document),
    CONSTRAINT ck_customers_document_not_blank CHECK (length(trim(document)) > 0),
    CONSTRAINT ck_customers_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT ck_customers_email_not_blank CHECK (length(trim(email)) > 0)
);
CREATE INDEX ix_customers_user_active ON customers(user_id, active);

CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customers(id),
    external_id VARCHAR(100) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency CHAR(3) NOT NULL,
    merchant VARCHAR(160) NOT NULL,
    status VARCHAR(20) NOT NULL,
    risk VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_transactions_external_id UNIQUE (external_id),
    CONSTRAINT ck_transactions_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_transactions_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_transactions_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT ck_transactions_risk CHECK (risk IN ('UNKNOWN', 'LOW', 'MEDIUM', 'HIGH'))
);
CREATE INDEX ix_transactions_customer_created ON transactions(customer_id, created_at DESC);
CREATE INDEX ix_transactions_status ON transactions(status);
CREATE INDEX ix_transactions_risk ON transactions(risk);
CREATE INDEX ix_transactions_merchant ON transactions(merchant);

CREATE TABLE transaction_status_history (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL REFERENCES transactions(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    changed_by UUID REFERENCES users(id),
    CONSTRAINT ck_transaction_history_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'))
);
CREATE INDEX ix_transaction_history_transaction ON transaction_status_history(transaction_id, changed_at);

CREATE TABLE idempotency_keys (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    key_value VARCHAR(255) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    response_status INTEGER NOT NULL,
    response_body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_idempotency_user_key UNIQUE (user_id, key_value)
);
CREATE INDEX ix_idempotency_expiry ON idempotency_keys(expires_at);
