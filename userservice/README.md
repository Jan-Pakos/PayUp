# userservice

Handles user registration, authentication (email/password and Google OAuth2), and profile retrieval. Issues signed JWTs that other services validate.

Runs on port **8081** in Docker (`8080` internally).

---

## Environment variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `JWT_SECRET` | **Yes** | `changeme-...` *(dev only)* | HMAC-SHA256 signing key. Must be at least 32 characters. Use a cryptographically random value in production and share the same value across all services. |
| `GOOGLE_CLIENT_ID` | For OAuth2 | — | Google Cloud Console OAuth2 client ID. Leave blank to disable Google login. |
| `GOOGLE_CLIENT_SECRET` | For OAuth2 | — | Google Cloud Console OAuth2 client secret. |
| `FRONTEND_URL` | No | `http://localhost:3000` | Base URL of your frontend. After Google OAuth2 login the user is redirected to `{FRONTEND_URL}/oauth2/callback?token={jwt}`. |
| `DB_USERNAME` | No | `postgres` | PostgreSQL username. |
| `DB_PASSWORD` | No | `postgres` | PostgreSQL password. |
| `SPRING_DATASOURCE_URL` | No | `jdbc:postgresql://localhost:5432/payup_users` | Full JDBC URL. Set automatically by Docker Compose to point to the `postgres-users` container. |

---

## Endpoints

### Auth — public

| Method | Path | Body | Description |
|---|---|---|---|
| `POST` | `/auth/signup` | `{ email, password, name }` | Register a new user. Returns a JWT. |
| `POST` | `/auth/signin` | `{ email, password }` | Sign in with email and password. Returns a JWT. |
| `GET` | `/oauth2/authorization/google` | — | Starts the Google OAuth2 login flow (browser redirect). |

#### Signup / Signin response
```json
{
  "token": "<jwt>",
  "email": "alice@example.com",
  "name": "Alice",
  "role": "ROLE_USER"
}
```

#### Validation rules
- `email` — must be a valid email address
- `password` — minimum 8 characters (signup only)
- `name` — must not be blank (signup only)

### Users — requires `Authorization: Bearer <token>`

| Method | Path | Description |
|---|---|---|
| `GET` | `/users/me` | Returns the authenticated user's profile. |

#### `/users/me` response
```json
{
  "id": 1,
  "email": "alice@example.com",
  "name": "Alice",
  "provider": "local",
  "role": "ROLE_USER"
}
```

---

## Features

### JWT authentication
All tokens are signed with HMAC-SHA256 using `JWT_SECRET`. The token contains `userId`, `email`, and `role` claims. Tokens expire after 24 hours (configurable via `app.jwt.expiration-ms`).

### Google OAuth2
- Flow starts at `GET /oauth2/authorization/google` — Spring Security handles the redirect and callback automatically.
- On success, the user is upserted in the database (3 cases: existing by Google ID, existing local account linked by email, or brand-new user created).
- A JWT is issued and the user is redirected to `{FRONTEND_URL}/oauth2/callback?token={jwt}`.
- The OAuth2 session is invalidated immediately after the redirect; subsequent requests use the JWT.

### Account linking
If a user signs up with email/password first, then later logs in with Google using the same email address, their Google identity is automatically linked to the existing account.

### Database migrations
Schema is managed by Flyway. Migrations live in `src/main/resources/db/migration/`. The `users` table is created on first boot.

---

## Running locally (without Docker)

```bash
# Start a local PostgreSQL instance and create the database
createdb payup_users

# Export required variables
export JWT_SECRET=dev-secret-that-is-long-enough-for-hs256

# Run
./mvnw spring-boot:run
```

The service starts on `http://localhost:8080`.
