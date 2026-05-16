CREATE TABLE wallets (
    id       BIGSERIAL      PRIMARY KEY,
    user_id  BIGINT         NOT NULL UNIQUE,
    balance  NUMERIC(19, 4) NOT NULL DEFAULT 0,
    currency VARCHAR(3)     NOT NULL DEFAULT 'USD',
    version  BIGINT         NOT NULL DEFAULT 0
);

CREATE TABLE wallet_transactions (
    id          BIGSERIAL      PRIMARY KEY,
    wallet_id   BIGINT         NOT NULL REFERENCES wallets(id),
    type        VARCHAR(10)    NOT NULL,
    amount      NUMERIC(19, 4) NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_wallet_transactions_wallet ON wallet_transactions (wallet_id, created_at DESC);
