# ImiTrade

Trading-platform backend. Spring Boot 3.5 / Java 25 / PostgreSQL / Flyway.

## Modules

| Feature    | Description                                                        |
|------------|-------------------------------------------------------------------|
| `auth`     | Registration (`POST /api/v1/auth/register`), login (`POST /api/v1/auth/login`) |
| `user`     | Current-user profile (`GET /api/v1/users/me`)                     |
| `security` | Stateless JWT auth, BCrypt, Spring Security 6 filter chain         |
| `common`   | Shared error envelope, exception handling                          |

## Authentication model

- **Stateless** — no HTTP sessions (`SessionCreationPolicy.STATELESS`).
- **BCrypt** password hashing (`BCryptPasswordEncoder`).
- **JWT (HS256)** issued on register/login; claims: `sub` = userId, `email`.
- `/api/v1/auth/**` and Swagger endpoints are **public**; everything else
  requires a valid `Authorization: Bearer <jwt>` header.
- Invalid/missing token → `401` with a JSON body (`code = UNAUTHENTICATED`).
- Bad credentials or unknown user → `401` (`code = INVALID_CREDENTIALS`)
  — login never reveals whether an e-mail is registered.
- Duplicate email/username → `409` (`EMAIL_ALREADY_EXISTS` / `USERNAME_ALREADY_EXISTS`).

### Initial balance

Every newly registered user starts with **500000.00** virtual money
(`UserService.INITIAL_BALANCE`), per business rule.

### Configuration (`application.yaml`)

```yaml
app:
  security:
    jwt:
      secret-key: ${APP_JWT_SECRET:<base64-key>}   # override in production!
      access-token-ttl: ${APP_JWT_TTL:86400000}    # ms (24h by default)
      issuer: imitrade
```

## Run

```bash
docker compose up -d        # PostgreSQL 17
./gradlew bootRun           # starts on :8080
```

## API (Swagger UI)

Once the app is running:

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs

### How to verify manually

1. **Register** — `POST /api/v1/auth/register`
   ```json
   { "email": "alice@example.com", "username": "alice", "password": "S3cret!pass" }
   ```
   Response → `201` with `{ "token": "...", "type": "Bearer", "expires_in": 86400 }`.

2. **Authorize in Swagger UI** — click **Authorize**, paste the token as
   `Bearer <token>` (or just the raw token).

3. **Call a protected endpoint** — `GET /api/v1/users/me` → `200` with the
   user profile (id, email, username, balance, createdAt).

4. **Login** — `POST /api/v1/auth/login` with the same credentials → new token.

5. **Negative checks:**
   - `GET /api/v1/users/me` with no token → `401 UNAUTHENTICATED`.
   - Same call with `Authorization: Bearer garbage` → `401 UNAUTHENTICATED`.
   - `POST /api/v1/auth/login` with a wrong password → `401 INVALID_CREDENTIALS`.
   - `POST /api/v1/auth/register` with a duplicate email → `409 EMAIL_ALREADY_EXISTS`.

## Tests

```bash
./gradlew test
```

| Suite                    | Type         | Coverage                                                                 |
|--------------------------|--------------|--------------------------------------------------------------------------|
| `JwtServiceTest`         | Unit         | Round-trip, tampered/expired/wrong-key/wrong-issuer/garbage tokens       |
| `UserServiceTest`        | Unit         | Register rules, BCrypt hashing, initial balance, not-found               |
| `AuthServiceTest`        | Unit         | Register/login happy path, bad password, unknown user, conflict         |
| `AuthIntegrationTest`    | Integration  | Register/login via MockMvc + H2, duplicate email/username, hashing, 400 |
| `SecurityAccessTest`     | Security     | Public/protected matrix, 401 without/with-invalid JWT, 200 with JWT      |

Integration/security tests use the **real security layer** (no mocking) and an
**in-memory H2** (PostgreSQL compatibility mode) via the `test` profile.

---

![schema.png](docks/db/schema.png)
