CREATE TABLE users (
    id          BIGSERIAL    PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255),
    name        VARCHAR(255) NOT NULL,
    provider    VARCHAR(50)  NOT NULL,
    provider_id VARCHAR(255),
    role        VARCHAR(50)  NOT NULL
);

CREATE INDEX idx_users_provider ON users (provider, provider_id);
