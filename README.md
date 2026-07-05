# ImiTrade

ImiTrade — платформа, имитирующая работу инвестиционного приложения.
Пользователь может покупать и продавать акции за виртуальную валюту, управлять
портфелем и просматривать историю операций. Актуальные рыночные цены автоматически
обновляются через интеграцию с MOEX ISS API.

Гостевой режим позволяет начать пользоваться приложением без регистрации.
Гостю выдаётся стартовый баланс 100 000 виртуальных кредитов и полный доступ
ко всем возможностям. При регистрации весь прогресс (баланс, портфель, история)
сохраняется, а гость получает бонус 400 000 кредитов.

## Features

- JWT-аутентификация (stateless, HS256) и авторизация
- Регистрация и вход пользователей (пароли хранятся в BCrypt)
- **Гостевой режим**: вход без регистрации через `X-Guest-Token`, стартовый баланс 100 000
- Конвертация гостя в зарегистрированного пользователя с бонусом 400 000 кредитов
- Каталог акций с пагинацией и фильтрами (тикер, название компании): ~50 MOEX-совместимых инструментов (голубые фишки РФ, эшелон-2, ETF)
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
├── guest/           # гостевой режим: создание гостя, X-Guest-Token аутентификация
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
| `users` | Пользователи: email, username, password_hash, balance, **is_guest**, **guest_token** |
| `stocks` | Каталог акций: ticker, company_name, exchange, current_price |
| `portfolio_positions` | Текущие позиции пользователя: user_id, stock_id, quantity, average_price |
| `transactions` | Источник истины (append-only): type (`BUY`/`SELL`), quantity, price, total_amount |

Связи: `portfolio_positions` и `transactions` ссылаются на `users` и `stocks`
через внешние ключи. Тип операции — PostgreSQL enum `transaction_type`.

Каталог `stocks` предзаполнен Flyway-миграциями: V2/V3 засевают 6 базовых
голубых фишек (SBER, GAZP, LKOH, ROSN, NVTK, YDEX) с ценами, **V5 расширяет
каталог до ~50 MOEX-совместимых инструментов** — ликвидные акции РФ
(GMKN, MGNT, TATN, VTBR, MOEX, PLZL, CHMF, NLMK, MTSS и др.), эшелон-2
(TCSG, OZON, HHRU, ENPG, ALRS, BANE …) и несколько ETF (SBMX, LQDT, TMOS,
TRUR, AKME). Все тикеры уникальны (`uk_stocks_ticker`), биржа — `MOEX`.
Цены в сид-данных — стартовые значения; `MarketDataScheduler` обновляет их
из MOEX ISS API во время работы приложения.

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

## Frontend

SPA-клиент живёт в каталоге `frontend/` и общается с этим backend по
`/api/v1/...`. Технологии: React 19 + TypeScript (strict), Vite 6, React Router 7,
Axios, TanStack Query 5, Material UI 7, React Hook Form + Zod, Sonner, Lucide.

### Запуск

```bash
cd frontend
npm install
npm run dev       # http://localhost:5173
npm run build     # tsc + vite build
npm run lint
```

Базовый URL backend берётся из `frontend/.env` (`VITE_API_URL`, по умолчанию
`http://localhost:8080`). Запускайте backend (`./gradlew bootRun`) одновременно с dev-сервером.

### Что реализовано

- **Auth**: JWT + гостевой режим. При первом заходе автоматически создаётся гость
  (`POST /api/v1/guest`), токен хранится в `localStorage`. JWT имеет приоритет над
  guest-token. Переключение User/Guest показывается в топбаре.
  - **Регистрация** (`POST /api/v1/auth/register`) — конвертирует текущего гостя в
    зарегистрированного пользователя: фронт автоматически прокидывает сохранённый
    `guestToken` в теле запроса, поэтому баланс, портфель и история операций
    сохраняются под тем же `user_id` (backend начисляет бонус +400 000). После
    конвертации guest-токен отбрасывается. Форма `pages/RegisterPage` (Username,
    Email, Password, Confirm Password) с zod-валидацией по контракту backend.
  - **Вход** (`POST /api/v1/auth/login`) — форма `pages/LoginPage` (Email, Password).
    После успеха `bootstrapAuth` и мутации тянут реальный профиль через
    `GET /api/v1/users/me` и инвалидируют `account`/`portfolio`/`transactions`.
  - **Выход** — frontend-only: очищает JWT и перезагружает страницу на `/dashboard`;
    при старте `bootstrapAuth` создаёт нового гостя, и приложение переходит в
    гостевой режим.
  - **Bootstrap** при старте приложения: при наличии JWT грузит профиль через
    `/users/me` (при протухшем токене — откатывается в guest-режим); иначе использует
    guest-token или автоматически создаёт гостя.
  - **Навигация**: гостю в топбаре показываются кнопки Sign In / Create Account;
    залогиненному — username + Logout.
  - 401-интерсептор `apiClient` не чистит гостевую сессию на `/auth/login` и
    `/auth/register` — иначе неудачный логин выкидывал бы гостя; эти ошибки
    показывает сама форма.
- **Dashboard**: сводка по аккаунту (`GET /api/v1/account`) — баланс, стоимость
  портфеля, PnL, число позиций.
- **Stocks**: каталог акций (`GET /api/v1/stocks`) с текущей ценой и кнопками
  Buy/Sell в каждой строке. Тикер в строке — ссылка на страницу детали акции.
- **Stock Detail**: детальная информация об акции на маршруте `/stocks/:ticker`
  (например, `/stocks/SBER`). Блоки: Header (компания, тикер, биржа), Price
  Block (текущая цена крупным шрифтом), User Position (позиция пользователя с
  количеством, средней ценой, текущей стоимостью и PnL, либо «You do not own
  this stock.»), Trading (кнопки Buy/Sell, переиспользующие `BuyStockDialog` /
  `SellStockDialog` из Trading), Company Information. Данные акции берутся
  фильтром `GET /api/v1/stocks?ticker=<ticker>` (case-insensitive exact match,
  без нового backend-эндпоинта); позиция — через Portfolio API. После сделки
  trading-мутации инвалидируют `account`/`stocks`/`portfolio`/`transactions`, поэтому
  Dashboard, Portfolio, Transactions и Stock Detail обновляются автоматически
  (каталог `stocks` теперь инвалидируется prefix-ключом, что захватывает и
  detail-запрос по тикеру). Loading — Skeleton, при ненайденной акции —
  Error State «Stock not found.» с кнопкой возврата в каталог. Кнопка Back to
  Stocks возвращает в каталог. Подготавливает почву для интеграции графика цены
  (MOEX candles) и истории котировок на следующих этапах.
- **Trading**: покупка и продажа акций (`POST /api/v1/trades/buy|sell`) через
  Material UI Dialog с валидацией количества (Zod). После сделки React Query
  автоматически инвалидирует `account`/`stocks`/`portfolio`/`transactions`,
  поэтому Dashboard и каталог обновляются без перезагрузки. Успех/ошибка — тосты
  (Sonner); текст ошибки приходит из backend.
- **Portfolio**: позиции пользователя (`GET /api/v1/portfolio`) в Material UI
  Table — тикер, компания, количество, средняя цена покупки, текущая цена,
  стоимость позиции, PnL (с цветовой индикацией) и кнопка Sell в каждой строке.
  Все финансовые показатели берутся из backend (PnL уже рассчитан, не
  пересчитывается на фронте). Кнопка Sell переиспользует существующий диалог
  продажи из Trading; после сделки портфель и Dashboard обновляются
  автоматически через `invalidateQueries`. Реализованы Skeleton при загрузке,
  блок ошибки с Retry и empty-state.
- **Transactions**: история торговых операций (`GET /api/v1/transactions`) в
  Material UI Table — дата, тип (BUY/SELL с цветовой индикацией), тикер,
  количество, цена, сумма сделки. `GET /transactions` возвращает Spring Page
  (по умолчанию size=20, DESC по `createdAt`); фронт читает `content`.
  Read-only фича без фильтров/сортировки/UI-пагинации. После сделки
  trading-мутации уже инвалидируют `queryKeys.transactions`, поэтому новые
  записи появляются автоматически. Реализованы Skeleton при загрузке, блок
  ошибки с Retry и empty-state.

### Единые стандарты отображения и UX

Фронт приведён к консистентному виду во всех фичах:

- **Форматирование** централизовано в `shared/utils/format.ts`: деньги — всегда
  `formatMoney` (группировка пробелом, точка-разделитель, без символа валюты),
  даты — всегда `formatDateTime` (`yyyy-mm-dd hh:mm`), PnL — всегда
  `formatProfitLoss` (знак `+`/`−` + цвет по теме MUI). Локальные копии и ручное
  форматирование (`.toFixed`, `toLocaleString`, `Intl`) в фичах отсутствуют.
- **UI-состояния** (loading/error/empty) унифицированы через переиспользуемые
  компоненты в `shared/components/`: `TableSkeleton` (скелетон таблицы),
  `StateError` (блок ошибки с кнопкой Retry, цвет `error.main` из темы — без
  хардкод-hex), `StateEmpty` (empty-state). Применяются в Stocks, Portfolio,
  Transactions и Account; inline-дубли этих блоков убрано.
- **React Query**: опции (`staleTime`/`refetchOnWindowFocus`/`retry`) заданы
  один раз как дефолты `QueryClient` и не дублируются в хуках. После сделок
  buy/sell инвалидируются `account`/`stocks`/`portfolio`/`transactions` —
  Dashboard, каталог, портфель, история и страница Stock Detail обновляются
  автоматически. Каталог `stocks` инвалидируется prefix-ключом, который
  захватывает и detail-запрос по тикеру (`['stocks','detail',ticker]`); сами
  рыночные цены от сделки не меняются (их обновляет планировщик backend), но
  инвалидация нужна для автообновления страницы Stock Detail после сделки.

`Account` — пока заглушка (страница с заголовком), её UI — следующий этап.
`Login` и `Register` реализованы полностью (формы с валидацией, мутациями и
переходом на Dashboard после успеха).

### Архитектура

Feature-based: `app/` (роутер + провайдеры), `pages/` (тонкие страницы),
`widgets/` (layout/sidebar/topbar), `features/<feature>/{api,model,types,ui,lib}`,
`shared/` (`api/`, `lib/`, `providers/`, `utils/`, `styles/`). Каждая фича —
закрытый модуль; общение между фичами — только через `shared/`. Псевдоним `@/` → `src/`.

Подробно правила и конвенции фронтенда — в `frontend/CLAUDE.md`.

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
| `POST` | `/api/v1/guest` | публичный | Создать гостевой аккаунт (возвращает `guestToken` и баланс) |
| `POST` | `/api/v1/auth/register` | публичный | Регистрация (опционально с `guestToken` для конвертации), возвращает JWT |
| `POST` | `/api/v1/auth/login` | публичный | Вход, возвращает JWT |
| `GET` | `/api/v1/users/me` | JWT или Guest | Профиль текущего пользователя |
| `GET` | `/api/v1/account` | JWT или Guest | Сводка по аккаунту |
| `GET` | `/api/v1/portfolio` | JWT или Guest | Текущие позиции с PnL |
| `GET` | `/api/v1/stocks` | JWT или Guest | Каталог акций (пагинация, фильтры) |
| `GET` | `/api/v1/stocks/{id}` | JWT или Guest | Акция по id |
| `POST` | `/api/v1/trades/buy` | JWT или Guest | Покупка акций |
| `POST` | `/api/v1/trades/sell` | JWT или Guest | Продажа акций |
| `GET` | `/api/v1/transactions` | JWT или Guest | История операций (фильтры) |

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

## Guest Mode

Приложение поддерживает два типа пользователей: **Registered** и **Guest**.
Бизнес-логика не зависит от типа: торговля, портфель, транзакции и аккаунт
работают одинаково.

### Создание гостя

`POST /api/v1/guest` — публичный endpoint. Создаёт пользователя с
`is_guest = true`, случайным `guest_token` (UUID) и балансом **100 000**.

```json
{
  "guestToken": "8c33bb2e-8d4b-4e0c-b57d-0dce5f6c4e3f",
  "balance": 100000
}
```

Frontend хранит `guestToken` (например, в `localStorage`) и передаёт его
в заголовке `X-Guest-Token` при каждом запросе.

### Идентификация гостя

Backend идентифицирует гостя **только** по `X-Guest-Token` (UUID). Не используются
IP, User-Agent или Cookies как единственный идентификатор. Это даёт стабильность
при смене сети, VPN и устройств.

### Регистрация гостя

`POST /api/v1/auth/register` с полем `guestToken` в теле запроса:

```json
{
  "email": "alice@example.com",
  "username": "alice",
  "password": "S3cret!pass",
  "guestToken": "8c33bb2e-8d4b-4e0c-b57d-0dce5f6c4e3f"
}
```

Происходит конвертация:
- Существующий гостевой пользователь получает email, username, password_hash
- `is_guest` → `false`, `guest_token` → `null`
- Баланс увеличивается на **400 000** (бонус за регистрацию)
- Весь прогресс (портфель, транзакции) сохраняется под тем же `user_id`
- Возвращается JWT — дальнейшая работа идёт через обычную JWT-аутентификацию

### Security

- `JwtAuthenticationFilter` проверяет `Authorization: Bearer <jwt>` первым
- `GuestAuthenticationFilter` проверяет `X-Guest-Token` вторым, если JWT отсутствует
- Если оба токена присутствуют, JWT имеет приоритет
- После регистрации `guestToken` становится недействительным

## Testing

```bash
./gradlew test
```

| Категория | Что покрывает | Примеры классов |
|---|---|---|
| **Unit** | Сервисный слой, парсинг JWT, логика PnL, валидации | `TradeServiceTest`, `AuthServiceTest`, `JwtServiceTest`, `PortfolioServiceTest`, `AccountServiceTest`, `GuestServiceTest` |
| **Integration** | Полный стек через MockMvc против БД | `AuthIntegrationTest`, `TradeIntegrationTest`, `PortfolioIntegrationTest`, `MarketDataSchedulerIntegrationTest`, `GuestIntegrationTest`, `GuestFlowIntegrationTest` |
| **Security** | Матрица доступа: 401/403 без токена, 200 с JWT или Guest | `SecurityAccessTest`, `StockSecurityTest`, `TradeSecurityTest`, `PortfolioSecurityTest`, `AccountSecurityTest`, `TransactionSecurityTest` |

Интеграционные и security-тесты используют реальную security-цепочку.
Часть тестов работает на H2, часть — на реальном PostgreSQL через Testcontainers.

## License

TODO: в репозитории нет файла лицензии. Добавьте файл `LICENSE` и выберите тип лицензии.
