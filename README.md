# ImiTrade

ImiTrade — платформа, имитирующая работу инвестиционного приложения.
Пользователь может покупать и продавать акции за виртуальную валюту, управлять
портфелем и просматривать историю операций. Актуальные рыночные цены автоматически
обновляются через интеграцию с MOEX ISS API.

## Features

- JWT-аутентификация (stateless, HS256) и авторизация
- Регистрация и вход пользователей (пароли хранятся в BCrypt)
- Каталог акций с пагинацией и фильтрами (тикер, название компании)
- Автоматическое обновление цен через MOEX ISS API
- Покупка и продажа акций по текущей рыночной цене
- Просмотр инвестиционного портфеля с незавершённым PnL
- Сводка по аккаунту (баланс, стоимость портфеля, прибыль/убыток)
- История операций с фильтрами
- OpenAPI-спецификация и Swagger UI
- Управление схемой БД через Flyway-миграции
- Docker Compose для локального PostgreSQL
- Тесты: unit, integration и security

## Tech stack

**Backend**

- Java 25
- Spring Boot 3.5.0
- Spring Security 6
- Spring Data JPA
- Spring Validation
- Flyway
- jjwt 0.12.6
- Lombok
- Gradle

**Database**

- PostgreSQL 17

**Testing**

- JUnit 5
- Mockito
- Spring Boot Test
- Testcontainers (PostgreSQL)
- H2 (для части тестов)

**Documentation**

- springdoc-openapi 2.8.9 (Swagger UI)

**Infrastructure**

- Docker
- Docker Compose

**External API**

- MOEX ISS API

## Architecture

Проект построен по **feature-based архитектуре**: каждый бизнес-модуль
(feature) инкапсулирует свой контроллер (`web`), сервис и репозиторий
(`domain`) и DTO (`dto`). Сквозная инфраструктура вынесена в модули
`security` и `common`.

```text
src/main/java/ImiTrade/
├── auth/            # регистрация и вход, выдача JWT
│   ├── web/         # AuthController
│   ├── domain/      # AuthService
│   └── dto/         # RegisterRequest, LoginRequest, AuthResponse
├── user/            # профиль текущего пользователя
│   └── web/ domain/ dto/
├── stocks/          # каталог акций (read-only) + обновление current_price
├── trading/         # покупка и продажа акций
├── portfolio/       # текущие позиции и незавершённый PnL
├── transaction/     # история операций (источник истины: append-only)
├── account/         # сводка по аккаунту (главный экран)
├── market/          # интеграция с MOEX ISS API и планировщик обновления цен
│   ├── client/      # MoexClient + DTO ответа
│   ├── domain/      # MarketDataService, MarketDataScheduler
│   └── config/      # MarketProperties, SchedulerProperties
├── security/        # фильтр JWT, SecurityFilterChain, обработчики 401/403
└── common/          # общий error-конверт, глобальная обработка исключений
```

Каждый защищённый эндпоинт получает аутентифицированного пользователя через
`@AuthenticationPrincipal`. Связи между сущностями хранятся как скалярные
id-колонки (без JPA-ассоциаций), денежные значения — `BigDecimal`.

## Database schema

Схема состоит из четырёх таблиц и управляется Flyway
(`src/main/resources/db/migration/`).

| Таблица | Назначение |
|---|---|
| `users` | Пользователи: email, username, password_hash, balance |
| `stocks` | Каталог акций: ticker, company_name, exchange, current_price |
| `portfolio_positions` | Текущие позиции пользователя: user_id, stock_id, quantity, average_price |
| `transactions` | Источник истины (append-only): type (`BUY`/`SELL`), quantity, price, total_amount |

Связи: `portfolio_positions` и `transactions` ссылаются на `users` и `stocks`
через внешние ключи. Тип операции — PostgreSQL enum `transaction_type`.

![Database schema](docks/db/schema.png)

## Getting started

1. **Клонировать репозиторий**

   ```bash
   git clone <repo-url>
   cd ImiTrade
   ```

2. **Запустить PostgreSQL** (требуется установленный Docker)

   ```bash
   docker compose up -d
   ```

   Поднимается контейнер `postgres:17-alpine` с БД `imitrade` на порту `5432`.

3. **Запустить приложение**

   ```bash
   ./gradlew bootRun
   ```

   Приложение стартует на `http://localhost:8080`.
   Flyway автоматически применит миграции при запуске.

4. **Открыть Swagger UI** по адресу ниже и выполнить запросы через UI.

## Configuration

### PostgreSQL (Docker Compose)

Параметры задаются в `docker-compose.yml`:

| Переменная | Значение по умолчанию |
|---|---|
| `POSTGRES_DB` | `imitrade` |
| `POSTGRES_USER` | `imitrade` |
| `POSTGRES_PASSWORD` | `imitrade` |
| Порт | `5432` |

### application.yaml

- DataSource: `jdbc:postgresql://localhost:5432/imitrade`
- JPA: `ddl-auto: validate` (схемой управляет Flyway, Hibernate только валидирует)
- Flyway: `locations: classpath:db/migration`, `baseline-on-migrate: true`

### Environment variables

| Переменная | Описание | Значение по умолчанию |
|---|---|---|
| `APP_JWT_SECRET` | Секрет JWT (обязательно к переопределению в проде) | встроенный demo-ключ |
| `APP_JWT_TTL` | Время жизни access-токена, мс | `86400000` (24 ч) |
| `APP_MOEX_BASE_URL` | Базовый URL MOEX ISS | `https://iss.moex.com/iss` |
| `APP_MARKET_SCHEDULER_ENABLED` | Включить планировщик обновления цен | `true` |
| `APP_MARKET_SCHEDULER_FIXED_RATE` | Период обновления цен, мс | `60000` |

## API Documentation

Интерактивная документация доступна после запуска приложения:

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs

Все эндпоинты версионированы префиксом `/api/v1`:

| Метод | Эндпоинт | Доступ | Описание |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | публичный | Регистрация, возвращает JWT |
| `POST` | `/api/v1/auth/login` | публичный | Вход, возвращает JWT |
| `GET` | `/api/v1/users/me` | JWT | Профиль текущего пользователя |
| `GET` | `/api/v1/account` | JWT | Сводка по аккаунту |
| `GET` | `/api/v1/portfolio` | JWT | Текущие позиции с PnL |
| `GET` | `/api/v1/stocks` | JWT | Каталог акций (пагинация, фильтры) |
| `GET` | `/api/v1/stocks/{id}` | JWT | Акция по id |
| `POST` | `/api/v1/trades/buy` | JWT | Покупка акций |
| `POST` | `/api/v1/trades/sell` | JWT | Продажа акций |
| `GET` | `/api/v1/transactions` | JWT | История операций (фильтры) |

## MOEX integration

Актуальные цены получаются из MOEX ISS API (публичный marketdata, без аутентификации):

- `MoexClient` запрашивает последнее значение цены (`LAST`) для тикера.
- `MarketDataScheduler` по расписанию `@Scheduled` (по умолчанию каждые 60 c)
  обходит все акции и обновляет колонку `current_price` в таблице `stocks`.
- Бизнес-модули (`TradeService`, `AccountService`) не обращаются к MOEX напрямую —
  они читают `current_price` из БД, поэтому всегда работают с последней
  сохранённой планировщиком ценой.

Параметры интеграции настраиваются в `application.yaml`
(`app.market.*`, см. раздел [Configuration](#configuration)).

## Testing

```bash
./gradlew test
```

| Категория | Что покрывает | Примеры классов |
|---|---|---|
| **Unit** | Сервисный слой, парсинг JWT, логика PnL, валидации | `TradeServiceTest`, `AuthServiceTest`, `JwtServiceTest`, `PortfolioServiceTest`, `AccountServiceTest` |
| **Integration** | Полный стек через MockMvc против БД | `AuthIntegrationTest`, `TradeIntegrationTest`, `PortfolioIntegrationTest`, `MarketDataSchedulerIntegrationTest` |
| **Security** | Матрица доступа: 401/403 без токена, 200 с JWT | `SecurityAccessTest`, `StockSecurityTest`, `TradeSecurityTest`, `PortfolioSecurityTest`, `AccountSecurityTest`, `TransactionSecurityTest` |

Интеграционные и security-тесты используют реальную security-цепочку.
Часть тестов работает на H2, часть — на реальном PostgreSQL через Testcontainers.

## License

TODO: в репозитории нет файла лицензии. Добавьте файл `LICENSE` и выберите тип лицензии.
