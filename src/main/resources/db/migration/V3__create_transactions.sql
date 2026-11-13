-- Every transfer posted by the TransferEngine, SUCCESS or FAILED.
-- Account numbers are plain columns (no FK) so a FAILED record can be stored
-- even when the requested account does not exist.

CREATE TABLE transactions (
    id                         BIGSERIAL     PRIMARY KEY,
    transaction_ref            VARCHAR(64)   NOT NULL,
    type                       VARCHAR(30)   NOT NULL,
    status                     VARCHAR(10)   NOT NULL,
    source_account_number      VARCHAR(20)   NOT NULL,
    destination_account_number VARCHAR(20)   NOT NULL,
    amount                     NUMERIC(19,2) NOT NULL,
    narration                  VARCHAR(140),
    reason                     VARCHAR(255),
    failure_reason             VARCHAR(50),
    initiated_by               VARCHAR(64)   NOT NULL,
    request_hash               VARCHAR(64)   NOT NULL,
    metadata                   JSONB,
    created_at                 TIMESTAMPTZ   NOT NULL DEFAULT now(),
    -- Duplicate-transfer guard: the database, not application code, is the final authority.
    CONSTRAINT uk_transactions_transaction_ref UNIQUE (transaction_ref),
    CONSTRAINT ck_transactions_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_transactions_status CHECK (status IN ('SUCCESS', 'FAILED'))
);

CREATE INDEX idx_transactions_source_created      ON transactions (source_account_number, created_at DESC);
CREATE INDEX idx_transactions_destination_created ON transactions (destination_account_number, created_at DESC);
