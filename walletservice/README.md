# walletservice

Manages user wallets and transaction history. Every authenticated user gets one wallet (created lazily on first access). Supports deposits, withdrawals, and full transaction history.

Runs on port **8082** in Docker (`8080` internally).

> **Auth note:** walletservice does **not** issue tokens. It only validates JWTs issued by userservice. `JWT_SECRET` must be identical in both services.

---

## Environment variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `JWT_SECRET` | **Yes** | `changeme-...` *(dev only)* | Must match the value set in userservice exactly. Used to verify token signatures — walletservice never generates tokens. |
| `DB_USERNAME` | No | `postgres` | PostgreSQL username. |
| `DB_PASSWORD` | No | `postgres` | PostgreSQL password. |
| `SPRING_DATASOURCE_URL` | No | `jdbc:postgresql://localhost:5432/payup_wallets` | Full JDBC URL. Set automatically by Docker Compose to point to the `postgres-wallets` container. |

---

## Endpoints

All endpoints require `Authorization: Bearer <token>` where the token was issued by userservice.

| Method | Path | Body | Description |
|---|---|---|---|
| `GET` | `/wallet` | — | Get the authenticated user's wallet. Creates it with a zero balance if it doesn't exist yet. |
| `POST` | `/wallet/deposit` | `{ amount, description? }` | Add funds to the wallet. |
| `POST` | `/wallet/withdraw` | `{ amount, description? }` | Remove funds from the wallet. Returns `422` if balance is insufficient. |
| `GET` | `/wallet/transactions` | — | Full transaction history for the wallet, newest first. |

#### Wallet response
```json
{
  "id": 1,
  "userId": 42,
  "balance": "150.0000",
  "currency": "USD"
}
```

#### Transaction response (within array)
```json
{
  "id": 7,
  "type": "CREDIT",
  "amount": "50.0000",
  "description": "Initial deposit",
  "createdAt": "2026-05-17T10:30:00Z"
}
```

#### Validation rules
- `amount` — must be `>= 0.01`; cannot be null

#### Error responses
| Status | Trigger |
|---|---|
| `400` | `amount` is missing or below `0.01` |
| `401` | Token is missing, expired, or has an invalid signature |
| `422` | Withdrawal amount exceeds current balance |

---

## Features

### Lazy wallet creation
A wallet is created automatically the first time a user calls any endpoint. There is no separate "create wallet" step.

### Optimistic locking
`Wallet.version` is annotated with `@Version` (Spring Data JDBC). Concurrent deposit/withdraw requests for the same wallet will fail fast with an optimistic locking exception rather than silently corrupting the balance.

### Transaction history
Every deposit and withdrawal records a `WalletTransaction` row (`CREDIT` or `DEBIT`) with an optional description and a UTC timestamp. The history endpoint returns all transactions sorted newest-first.

### Separate database
walletservice owns its own `payup_wallets` PostgreSQL database. It has no knowledge of the `payup_users` database — user identity is carried entirely in the JWT (`userId` claim).

### Database migrations
Schema is managed by Flyway. Migrations live in `src/main/resources/db/migration/`. The `wallets` and `wallet_transactions` tables are created on first boot.

---

## Running locally (without Docker)

```bash
# Start a local PostgreSQL instance and create the database
createdb payup_wallets

# Export required variables — must match userservice's JWT_SECRET
export JWT_SECRET=dev-secret-that-is-long-enough-for-hs256

# Run
./mvnw spring-boot:run
```

The service starts on `http://localhost:8080`.
