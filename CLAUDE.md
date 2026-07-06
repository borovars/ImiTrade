# ImiTrade — Project Overview

ImiTrade is a backend for a virtual stock trading platform.

Users receive virtual money and can buy and sell stocks.
The system maintains portfolios and transaction history.

Current state:
- Authentication (register/login) ✓
- Stock catalog (read-only) ✓ — ~50 MOEX-compatible instruments (blue chips, second-tier, ETFs)
- Trading (buy/sell) ✓
- Portfolio (read view with live PnL) ✓
- Account summary (balance + live portfolio aggregates) ✓
- Transaction history (read-only, filterable) ✓
- Live price updates from MOEX ISS API (scheduled) ✓
- **Lot-based trading** (lot size per stock, synced from MOEX; trades are placed in lots, shares remain the source of truth) ✓
- **Guest Mode** (create guest, X-Guest-Token auth, convert to registered user with bonus) ✓

Main business entities:
- User
- Stock
- PortfolioPosition
- Transaction

Transactions are the source of truth.
PortfolioPosition stores aggregated current holdings.
`stocks.current_price` is kept in sync with MOEX by a scheduler; all trades and
PnL/account aggregates read the persisted price — business modules never call MOEX directly.
## Tech Stack

| Layer         | Technology                                       |
|---------------|--------------------------------------------------|
| Language      | Java 25                                           |
| Framework     | Spring Boot 3.5                                   |
| Build         | Gradle (Groovy DSL) — `./gradlew bootRun` / `test` |
| Database      | PostgreSQL 17 (via Docker Compose)                |
| Migration     | Flyway (classpath:db/migration)                   |
| ORM           | Spring Data JPA + Hibernate (ddl-auto = validate)  |
| Security      | Spring Security 6 (stateless JWT, BCrypt)          |
| JWT           | jjwt 0.12.6 (HS256)                              |
| API Docs      | springdoc-openapi 2.8.9 (Swagger UI at /swagger-ui.html) |
| Lombok        | Compile-time annotation processing               |
| Validation    | Jakarta Bean Validation (spring-boot-starter-validation) |
| HTTP client   | Spring `RestClient` (MOEX marketdata calls)      |
| Test DB       | Testcontainers (PostgreSQL 17) + H2 in-memory (PostgreSQL compatibility mode) |

## Architecture

**Feature-based modular monolith** — each business domain is a top-level package under `src/main/java/ImiTrade/`.

```
src/main/java/ImiTrade/
├── Main.java                    # @SpringBootApplication entry point
├── auth/                        # Registration & login
│   ├── web/AuthController.java  # REST controller
│   ├── domain/AuthService.java  # Application service
│   └── dto/                     # AuthResponse, RegisterRequest, LoginRequest, CurrentUserResponse
├── user/                        # User aggregate
│   ├── web/UserController.java
│   ├── domain/UserService.java, User.java, UserRepository.java
│   └── (no dto — reuses auth dto CurrentUserResponse)
├── stocks/                      # Stock catalog (read-only)
│   ├── web/StockController.java
│   ├── domain/StockService.java, Stock.java, StockRepository.java, StockSpecifications.java
│   └── dto/StockResponse.java
├── transaction/                 # Transaction aggregate (entity + repo + enum + history read)
│   ├── web/TransactionController.java  # GET /api/v1/transactions (filterable, paginated)
│   ├── domain/TransactionService.java, Transaction.java, TransactionRepository.java,
│   │           TransactionSpecifications.java, TransactionType.java
│   └── dto/TransactionResponse.java
├── trading/                     # Buy/sell orchestration
│   ├── web/TradeController.java
│   ├── domain/TradeService.java
│   └── dto/BuyStockRequest.java, SellStockRequest.java, TradeResponse.java
├── portfolio/                   # Holdings read model
│   ├── web/PortfolioController.java
│   ├── domain/PortfolioService.java, PortfolioPosition.java, PortfolioPositionRepository.java
│   └── dto/PortfolioResponse.java
├── account/                     # Account summary (main screen)
│   ├── web/AccountController.java
│   ├── domain/AccountService.java  # computes portfolioValue/totalAssets/profitLoss in memory
│   └── dto/AccountResponse.java
├── market/                      # MOEX ISS integration + price-refresh scheduler
│   ├── client/MoexClient.java + dto/   # RestClient call to MOEX marketdata
│   ├── domain/MarketDataService.java, MarketDataScheduler.java
│   └── config/MarketClientConfig.java, MarketProperties.java, SchedulerProperties.java
├── security/                    # JWT infrastructure
│   ├── SecurityFilterChainConfig.java
│   ├── JwtService.java, JwtProperties.java
│   ├── JwtAuthenticationFilter.java, JwtAuthentication.java
│   ├── GuestAuthenticationFilter.java  # X-Guest-Token authentication
│   ├── JwtAuthenticationEntryPoint.java, JwtAccessDeniedHandler.java
│   └── AuthenticatedUser.java
├── guest/                       # Guest user creation
│   ├── web/GuestController.java
│   ├── domain/GuestService.java
│   └── dto/GuestResponse.java
└── common/                      # Shared utilities
    ├── web/GlobalExceptionHandler.java, ApiResponse.java, ErrorCodes.java
    └── exception/               # Domain exception hierarchy
```

### Module Structure Convention

Each feature module follows a strict **3-layer** pattern:

```
<feature>/
├── web/         # @RestController, request mapping, Swagger annotations
├── domain/      # @Service (application service), @Entity, Repository, Specifications
└── dto/         # Java records for request/response — never expose entities directly
```

Cross-module dependencies flow **inward**: controllers → services → repositories. A service in one module may call services in other modules (e.g., `AuthService` → `UserService`, `TradeService` → `UserService` + `StockService`). Controllers never call other controllers.

The `transaction/` package holds the Transaction entity, repository, enum, the
history read model (`TransactionService` + `TransactionController` + DTO), and the
JPA `Specification`s used for filtering. The buy/sell **write** orchestration lives
in `trading/`. This separation keeps the write model (transaction) distinct from
the application service (trade).

### Package Naming

- Top-level package: `ImiTrade` (capital I, capital T — matches project name)
- Feature packages: lowercase (`auth`, `user`, `stocks`, `security`, `common`)

## Database Schema

Managed exclusively by **Flyway**. Never modify `ddl-auto` from `validate`.

| Table               | Purpose                        |
|---------------------|--------------------------------|
| `users`              | User accounts, balance, auth, **is_guest**, **guest_token** |
| `stocks`             | Read-only stock catalog (current_price added by V3, extended to ~50 MOEX tickers by V5, lot_size added by V6) |
| `portfolio_positions`| User stock holdings                                              |
| `transactions`      | Buy/sell history                                                 |

**Migrations**: V1 created all four tables (+ `transaction_type` enum), V2 seeded 6 stocks, V3 added `stocks.current_price NUMERIC(19,4)`, **V4 added `users.is_guest` and `users.guest_token`, made `email/username/password_hash` nullable for guests**, **V5 extended the stocks catalog to ~50 MOEX-compatible instruments (blue chips + second-tier issuers + ETFs); schema unchanged — seed data only**, **V6 added `stocks.lot_size INTEGER NOT NULL` and backfilled real MOEX `LOTSIZE` values for the ~50 catalog tickers**.

**Naming**: PostgreSQL snake_case columns mapped to Java camelCase fields via JPA `@Column(name = "...")`.

## API Conventions

- Base path: `/api/v1/`
- Public endpoints: `/api/v1/auth/**`, `/api/v1/guest`, `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**`
- JWT-protected endpoints: `/api/v1/users/**`, `/api/v1/stocks/**`, `/api/v1/trades/**`, `/api/v1/portfolio`, `/api/v1/account`, `/api/v1/transactions`
- **Guest access**: all JWT-protected endpoints also accept `X-Guest-Token` header (UUID) for guest users
- All other endpoints require `Authorization: Bearer <jwt>` or `X-Guest-Token` (enforced via `anyRequest().authenticated()`)
- Standard error envelope (`ApiResponse` record): `{ timestamp, status, error, code, message, path, details }`
- Error codes are constants in `common/web/ErrorCodes.java` — always use `UPPER_SNAKE_CASE`

## Code Style & Conventions

### Java

- **Records** for all DTOs (`AuthResponse`, `RegisterRequest`, `StockResponse`, etc.)
- **Entities**: JPA `@Entity` with Lombok `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`
    - No-args constructor: `AccessLevel.PROTECTED`
    - All-args constructor: `AccessLevel.PRIVATE`
- **Services**: `@Slf4j @Service @RequiredArgsConstructor`, constructor injection only (no field injection)
- **Controllers**: `@RestController @RequiredArgsConstructor`, return `ResponseEntity<T>`
- **Exceptions**: Domain exceptions extend `RuntimeException` or `ResourceAlreadyExistsException`. Handled centrally by `GlobalExceptionHandler`
- **Swagger**: Every endpoint has `@Operation`, `@ApiResponses`, and `@Tag` on the controller
- **Transactions**: `@Transactional` on service methods; `@Transactional(readOnly = true)` for reads
- **Logging**: `log.debug()` for request-level tracing, `log.info()` for business events
- **No business logic in controllers** — controllers delegate to services
- **Money**: always `BigDecimal` with scale 4 and `RoundingMode.HALF_UP`; never `double`. PnL is computed in-memory and never persisted.

### DTO → Entity Mapping

DTOs contain a static `from(Entity e)` factory method (e.g., `StockResponse.from(stock)`). This is the only place entity → DTO conversion happens.

### Exception Pattern

```
common/exception/
├── ResourceAlreadyExistsException   # Abstract base for 409 conflicts
├── EmailAlreadyExistsException       # Extends ResourceAlreadyExistsException
├── UsernameAlreadyExistsException    # Extends ResourceAlreadyExistsException
├── UserNotFoundException              # 404
├── StockNotFoundException             # 404
├── InvalidQuantityException            # 400
├── InsufficientBalanceException       # 400
├── InsufficientStockQuantityException # 400
├── PortfolioPositionNotFoundException # 404
├── MarketDataUnavailableException     # 503 (MOEX ISS unreachable / no price)
├── InvalidTickerException             # 404 (ticker not found on MOEX)
├── InvalidCredentialsException       # 401 (used in login to prevent user enumeration)
├── InvalidGuestTokenException         # 401 (invalid or missing X-Guest-Token)
├── GuestAlreadyRegisteredException    # 409 (guest already converted to registered)
└── AuthException                     # Base for auth-related errors
```

Every new domain exception must be:
1. Defined in `common/exception/`
2. Registered with an error code in `ErrorCodes.java`
3. Mapped to an HTTP status in `GlobalExceptionHandler.java`

### Error Codes

All error code constants live in `common/web/ErrorCodes.java`:

| Code | Exception | HTTP |
|------|-----------|------|
| `VALIDATION_ERROR` | `MethodArgumentNotValidException` | 400 |
| `EMAIL_ALREADY_EXISTS` | `EmailAlreadyExistsException` | 409 |
| `USERNAME_ALREADY_EXISTS` | `UsernameAlreadyExistsException` | 409 |
| `INVALID_CREDENTIALS` | `InvalidCredentialsException` | 401 |
| `UNAUTHENTICATED` | Spring `AuthenticationException` | 401 |
| `ACCESS_DENIED` | Spring `AccessDeniedException` | 403 |
| `USER_NOT_FOUND` | `UserNotFoundException` | 404 |
| `STOCK_NOT_FOUND` | `StockNotFoundException` | 404 |
| `INVALID_QUANTITY` | `InvalidQuantityException` | 400 |
| `INSUFFICIENT_BALANCE` | `InsufficientBalanceException` | 400 |
| `INSUFFICIENT_STOCK_QUANTITY` | `InsufficientStockQuantityException` | 400 |
| `PORTFOLIO_POSITION_NOT_FOUND` | `PortfolioPositionNotFoundException` | 404 |
| `MARKET_DATA_UNAVAILABLE` | `MarketDataUnavailableException` | 503 |
| `INVALID_TICKER` | `InvalidTickerException` | 404 |
| `INVALID_GUEST_TOKEN` | `InvalidGuestTokenException` | 401 |
| `GUEST_ALREADY_REGISTERED` | `GuestAlreadyRegisteredException` | 409 |
| `INTERNAL_ERROR` | catch-all `Exception` | 500 |

Note: most codes are constants in `ErrorCodes.java`, but a few are still inlined
as string literals in `GlobalExceptionHandler` (e.g. `USER_NOT_FOUND`). Prefer
adding a constant in `ErrorCodes` for new codes.

## Testing

- Framework: JUnit 5 (via spring-boot-starter-test)
- DB strategy: tests either use H2 (`test` profile, PostgreSQL compatibility mode, Flyway disabled, schema in `src/test/resources/schema.sql`) or real PostgreSQL via Testcontainers (`PostgresTestBase`) — the latter is required for the `transaction_type` PostgreSQL enum
- Test types:
    - **Unit tests** (`*Test.java`): Service layer with Mockito mocks
    - **Integration tests** (`*IntegrationTest.java`): MockMvc + real security + H2 or Testcontainers
    - **Security tests** (`*SecurityTest.java`): Full filter chain verification (401 without/invalid JWT, 200 with JWT or X-Guest-Token, per-endpoint matrix)
- Test suites:
    - **auth**: `AuthServiceTest`, `AuthIntegrationTest`
    - **user**: `UserServiceTest`
    - **guest**: `GuestServiceTest`, `GuestIntegrationTest`, `GuestFlowIntegrationTest`
    - **security**: `JwtServiceTest`, `SecurityAccessTest`
    - **stocks**: `StockServiceTest`, `StockRepositoryTest`, `StockControllerTest`, `StockSecurityTest`
    - **trading**: `TradeServiceTest`, `TradeIntegrationTest`, `TradeSecurityTest`
    - **portfolio**: `PortfolioPositionRepositoryTest`, `PortfolioServiceTest`, `PortfolioControllerTest`, `PortfolioIntegrationTest`, `PortfolioSecurityTest`
    - **account**: `AccountServiceTest`, `AccountControllerTest`, `AccountSecurityTest`
    - **transaction**: `TransactionServiceTest`, `TransactionRepositoryTest`, `TransactionControllerTest`, `TransactionSecurityTest`
    - **market**: `MoexClientTest`, `MarketDataServiceTest`, `MarketDataSchedulerTest`, `MarketDataSchedulerIntegrationTest`
- Integration/security tests that touch the `transaction_type` enum extend `PostgresTestBase` (Testcontainers, PostgreSQL 17). Auth and simpler controller tests run on H2. The full suite runs sequentially (`maxParallelForks = 1`) because several classes spin up their own Testcontainers PostgreSQL.
- Run: `./gradlew test`

## Configuration

- Production config: `src/main/resources/application.yaml`
- Test config: `src/test/resources/application-test.yaml`
- Server port: 8080
- Environment variables (all optional, have dev defaults):
    - `APP_JWT_SECRET` — JWT signing secret (must override in prod)
    - `APP_JWT_TTL` — access token lifetime in ms (default `86400000`, 24h)
    - `APP_MOEX_BASE_URL` — MOEX ISS root (default `https://iss.moex.com/iss`)
    - `APP_MARKET_SCHEDULER_ENABLED` — enable price-refresh scheduler (default `true`)
    - `APP_MARKET_SCHEDULER_FIXED_RATE` — refresh period in ms (default `60000`)

## Common Commands

```bash
./gradlew bootRun           # Start the application
./gradlew test              # Run all tests
docker compose up -d        # Start PostgreSQL
docker compose down -v      # Stop PostgreSQL + delete data volume
```

## Git Workflow

- Main branch: `master`
- Feature branches are merged via pull requests
- Commit messages: **Russian language** (e.g., "добавлена схема бд и ридми файл")
- Branch naming: feature-based, lowercase (e.g., `stock`, `auth`, `db`)

## Business Rules

- Initial balance: every new user receives `500000.0000` virtual money (`UserService.INITIAL_BALANCE`)
- **Guest balance**: every new guest receives `100000.0000` virtual money (`UserService.GUEST_INITIAL_BALANCE`)
- **Guest registration bonus**: `400000.0000` (`UserService.GUEST_REGISTRATION_BONUS`) added when a guest converts to a registered user
- **Guest conversion**: sets `is_guest = false`, `guest_token = null`, fills `email/username/password_hash`, adds bonus to existing balance. Portfolio and transactions are preserved under the same `user_id`.
- Login never reveals whether an email is registered (same 401 for "wrong password" and "no such user")
- Stocks are read-only: seeded by Flyway, not modifiable through the API
- **Lots**: every stock has a `lot_size` (shares per lot, synced from MOEX `securities.LOTSIZE` by the scheduler). Trading is done in **lots**: the client sends `{ stockId, lots }`, the backend computes `quantity = lots × lotSize` and stores the share `quantity` on the transaction/position (lots are never persisted — shares are the single source of truth). `quantity % lotSize == 0` is guaranteed by construction; `lots > 0` and a positive `lotSize` are validated, reusing `INVALID_QUANTITY` for violations
- **Buy**: validates lots > 0 + lotSize > 0, computes quantity = lots × lotSize, checks balance ≥ price × qty, saves BUY transaction, upserts position with weighted-average price: `(oldQty×oldAvg + buyQty×price) / (oldQty+buyQty)` (scale 4, HALF_UP), debits balance
- **Sell**: validates lots > 0 + lotSize > 0, computes quantity = lots × lotSize, loads the stock first (so lotSize is known before the position check), checks position exists + sufficient shares, saves SELL transaction, removes position at quantity 0 (otherwise decrements), credits balance
- Virtual-money settlement: `balance -= total` on buy, `balance += total` on sell
- **PnL** = `(currentPrice − averagePrice) × quantity` (scale 4, HALF_UP), computed in-memory per request, never persisted
- **Account aggregates** (computed in-memory in `AccountService`, never persisted):
    - `portfolioValue = Σ(quantity × currentPrice)` (scale 4, HALF_UP)
    - `profitLoss     = Σ((currentPrice − averagePrice) × quantity)` (scale 4, HALF_UP)
    - `totalAssets    = balance + portfolioValue` (scale 4, HALF_UP)
    - `positionsCount = number of current portfolio_positions`
- **MOEX price flow**: `MoexClient.getMarketSnapshot` fetches the `LAST` price and `LOTSIZE` for a ticker in a single MOEX ISS call (`iss.only=marketdata,securities` — `LOTSIZE` lives in the `securities` block, not `marketdata`); `MarketDataScheduler` (`@Scheduled`, default every 60s) updates `stocks.current_price` via `StockRepository.updateCurrentPrice` and, when MOEX returns a lot size, `stocks.lot_size` via `StockRepository.updateLotSize` (a missing lot size keeps the persisted value — the contract forbids making one up); trade/account/portfolio logic reads `current_price`/`lot_size` from the DB, so it always uses the last value the scheduler persisted
- Transactions are the source of truth for trade history

## DO / DON'T

- **DO** use Java `record` for all DTOs
- **DO** use Lombok for entities and services (constructor injection via `@RequiredArgsConstructor`)
- **DO** add Swagger `@Operation` + `@ApiResponses` to every endpoint
- **DO** add Javadoc to public classes and methods
- **DO** use `@Transactional(readOnly = true)` for read-only service methods
- **DO** follow the `web/domain/dto` package structure for new modules
- **DO** register new error codes in `ErrorCodes.java`
- **DO** map new exceptions in `GlobalExceptionHandler.java`
- **DON'T** expose JPA entities directly in API responses — always use DTOs
- **DON'T** put business logic in controllers
- **DON'T** change `ddl-auto` from `validate` (schema is managed by Flyway)
- **DON'T** use field injection — constructor injection only
- **DON'T** create tables or modify schema outside of Flyway migrations


## Agent Instructions

Read this file before exploring the codebase.

Do not scan unrelated modules unless necessary.

Assume the information in this file is authoritative.

Prefer existing project patterns over introducing new approaches.

Minimize token usage and code changes.

Implement only the requested feature and avoid speculative improvements.
