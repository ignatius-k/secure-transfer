-- Single-currency system: currency is intentionally not modelled.
-- customer_id is UNIQUE => one account per customer (for now).
-- customer_id is NULL for internal accounts (e.g. reconciliation suspense).

CREATE TABLE accounts (
    id             BIGSERIAL     PRIMARY KEY,
    account_number VARCHAR(20)   NOT NULL,
    account_name   VARCHAR(100)  NOT NULL,
    customer_id    BIGINT        REFERENCES users (id),
    balance        NUMERIC(19,2) NOT NULL DEFAULT 0,
    status         VARCHAR(20)   NOT NULL,
    version        BIGINT        NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uk_accounts_account_number UNIQUE (account_number),
    CONSTRAINT uk_accounts_customer_id    UNIQUE (customer_id),
    -- Database-level safety net: a balance can never go negative,
    -- even if application logic is bypassed.
    CONSTRAINT ck_accounts_balance_non_negative CHECK (balance >= 0),
    CONSTRAINT ck_accounts_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'FROZEN', 'CLOSED'))
);
