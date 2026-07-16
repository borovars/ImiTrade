# ImiTrade

ImiTrade — платформа, имитирующая работу инвестиционного приложения.
Пользователь может покупать и продавать акции за виртуальную валюту, управлять
портфелем и просматривать историю операций. Актуальные рыночные цены автоматически
обновляются через интеграцию с MOEX ISS API.

Гостевой режим позволяет начать пользоваться приложением без регистрации.
Гостю выдаётся стартовый баланс 5 000 виртуальных кредитов и полный доступ
ко всем возможностям. При регистрации весь прогресс (баланс, портфель, история)
сохраняется, а гость получает бонус 20 000 кредитов.

## Features

- JWT-аутентификация (stateless, HS256) и авторизация
- Регистрация и вход пользователей (пароли хранятся в BCrypt)
- **Гостевой режим**: вход без регистрации через `X-Guest-Token`, стартовый баланс 5 000
- Конвертация гостя в зарегистрированного пользователя с бонусом 20 000 кредитов
- Каталог акций с пагинацией и фильтрами (тикер, название компании): ~50 MOEX-совместимых инструментов (голубые фишки РФ, эшелон-2, ETF)
- **Карточка компании**: описание, сектор экономики, официальный сайт и логотип по
  тикеру — backend единый источник информации о компании, без обращения к внешним API
- **Исторический график цены**: профессиональный интерактивный **линейный** график
  (close price) на странице акции, полностью контролируемый React-компонентом на
  **Lightweight Charts** (TradingView). Линия цвета `#3dba8d` с прозрачным градиентом,
  чистый фон без сетки. Модель «кнопка = интервал свечи» (стиль Т-Инвестиций / MOEX):
  1D — дневные свечи (−3 месяца), 1W — недельные (−5 месяцев), 1M — месячные (−3 года),
  1Y — квартальные (−10 лет). **Колесо мыши масштабирует график относительно точки под
  курсором** (zoom in/out, как в торговых терминалах), а **перетаскивание ЛКМ**
  панорамирует и при выходе за левый край догружает более старые данные **тем же
  интервалом** (бесконечный скролл в прошлое), вправо в будущее двигать нельзя. При
  наведении показываются **вертикальная пунктирная направляющая** (чёрная, через весь
  холст), **круглый маркер** в точке пересечения линии графика с направляющей
  (фирменный цвет `#3dba8d`, плавная CSS-анимация появления) и **tooltip** с датой и
  ценой (фиксированное смещение справа-снизу от курсора). Lazy loading с защитой от
  бесконечных запросов и циклом автодогрузки при сильном отдалении. Данные —
  `GET /api/v1/stocks/{ticker}/history?period=&from=` через backend (backend сам идёт
  в MOEX ISS Candles и возвращает OHLCV; фронт рисует только `close`).
- Автоматическое обновление цен и размеров лотов через MOEX ISS API
- Торговля лотами (размер лота синхронизируется с MOEX; `quantity = lots × lotSize` считается на backend, акции остаются источником истины)
- Покупка и продажа акций по текущей рыночной цене
- Просмотр инвестиционного портфеля с незавершённым PnL
- **История стоимости портфеля**: профессиональный **линейный** график изменения
  рыночной стоимости всех активов пользователя во времени на странице Портфель.
  График стоимости (`Σ количество акции × её рыночная цена`) для каждой временной
  точки реконструируется **на backend** — replay транзакций пользователя против
  исторических цен MOEX (через тот же сервис истории цены акции, без второго
  источника рыночных данных). Стоимость портфеля, а не баланс — это график
  инвестиционных активов; точки, когда пользователь ничего не держал, пропускаются.
  Каждый тикер запрашивается у MOEX **один раз** (локальный кэш внутри расчёта,
  без N×M запросов). Фронт получает готовый временной ряд через
  `GET /api/v1/portfolio/history?period=` и рисует его переиспользованием того же
  компонента графика (Lightweight Charts, линия `#3dba8d` + градиент, чистый фон,
  периоды 1D/1W/1M/1Y, crosshair/tooltip/marker). Empty State с кнопкой
  «Перейти к акциям» — когда портфель пуст. Не персистится.
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
├── stocks/          # каталог акций (read-only) + обновление current_price + logoUrl (StockLogoResolver)
│   ├── integration/moex/  # MoexHistoryClient + MoexHistoryMapper + dto/ (candles.json)
│   ├── service/           # StockHistoryService, HistoryPeriod (история цены, не персистится)
│   ├── web/               # StockController, StockHistoryController
│   └── dto/               # StockResponse, CandleResponse
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
| `stocks` | Каталог акций: ticker, company_name, exchange, current_price, lot_size, description, sector, website |
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
из MOEX ISS API во время работы приложения. **V6 добавляет `lot_size`
(размер лота для каждой акции) и заполняет его реальными значениями из
MOEX `securities.LOTSIZE`; планировщик также синхронизирует `lot_size`
одновременно с ценой.** **V7 добавляет расширенную информацию о компании
(`description`, `sector`, `website`) и заполняет её для всех ~50 тикеров
реальными данными.**

### Информация о компании и логотипы

Backend — единственный источник информации о компании. Для каждой акции
хранятся `description` (краткое описание, 2–4 предложения), `sector`
(сектор экономики) и `website` (официальный сайт) в таблице `stocks`.
Эти поля попадают в `StockResponse` и доступны фронтенду без дополнительных
запросов к внешним сервисам.

**Логотипы не хранятся в БД.** SVG-файлы лежат в
`src/main/resources/static/logos/`, имя файла совпадает с тикером
(`SBER.svg`, `GAZP.svg`, …), плюс `default.svg` как запасной вариант.
Spring Boot раздаёт их как статические ресурсы по адресу `/logos/{ticker}.svg`
(публично, без аутентификации). Поле `logoUrl` в `StockResponse` **не
сохраняется** в БД — оно вычисляется на backend компонентом
`StockLogoResolver` (один раз на старте сканирует каталог логотипов и
кеширует результат): если для тикера есть SVG, возвращается
`/logos/{TICKER}.svg`, иначе `/logos/default.svg`. Изображения в
PostgreSQL/BLOB/Base64 не используются.

### Лоты

Каждая акция имеет размер лота (`lot_size`, число акций в одном лоте).
Торговля идёт **в лотах**: клиент отправляет `{ "stockId": 1, "lots": 3 }`,
backend вычисляет `quantity = lots × lotSize` и хранит именно число акций
в `portfolio_positions.quantity` и `transactions.quantity`. Лоты нигде не
сохраняются — единственный источник истины это количество акций. Все
вычисления `quantity = lots × lotSize` выполняются только на backend;
кратность `lotSize` гарантируется конструкцией.

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
Axios, TanStack Query 5, Material UI 7, React Hook Form + Zod, Sonner, Lucide,
Lightweight Charts 5 (линейный график цены акции на странице детали, только close;
OHLCV отсекается на фронте). Мини-графики (sparkline) в каталоге акций рисуются
чистым SVG без библиотек.

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
    сохраняются под тем же `user_id` (backend начисляет бонус +20 000). После
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
  портфеля, PnL, число позиций — через `AccountSummary` (карточки с
  info-подсказками), а под ним компактный **панель портфеля**
  (`DashboardPortfolioPanel`, `GET /api/v1/portfolio`) с позициями: тикер,
  компания, количество, средняя/текущая цена, изменение цены в абсолют и
  процент к средней, PnL. Панель read-only — без кнопок торговли, это сводный
  обзор; торговля ведётся со страницы «Портфель».
- **Stocks**: каталог акций (`GET /api/v1/stocks`) с текущей ценой, размером лота
  (колонка Lot Size), кнопками Buy/Sell в каждой строке и **мини-графиком цены**
  (sparkline за последний год) в отдельной колонке. **Вся строка кликабельна** —
  клик по любой ячейке открывает страницу детали акции (`/stocks/{ticker}`);
  клик по кнопкам Buy/Sell и по тикеру не вызывает переход (`stopPropagation`).
  Sparkline рисуется чистым SVG без графических
  библиотек (10 строк в таблице не должны тянуть 10 canvas'ов lightweight-charts);
  данные берутся из того же эндпоинта истории цены
  (`GET /api/v1/stocks/{ticker}/history`, period=1M), цвет линии — по годовому
  тренду (зелёный/красный).
- **Stock Detail**: детальная информация об акции на маршруте `/stocks/:ticker`
  (например, `/stocks/SBER`). Блоки: Header (**логотип компании** через
  `<Avatar src=…>` с fallback на `default.svg`, компания, тикер, биржа,
  **сектор**), Price Block (текущая цена крупным шрифтом), User Position
  (позиция пользователя с количеством, средней ценой, текущей стоимостью и
  PnL, либо «You do not own this stock.»), Trading (кнопки Buy/Sell,
  переиспользующие `BuyStockDialog` / `SellStockDialog` из Trading),
  **About** (описание компании), Company Information (включая Lot Size,
  Sector и Website-ссылкой). Логотип и все данные компании берутся из
  payload акции (`logoUrl`/`description`/`sector`/`website`) — без обращения
  к внешним API. Данные акции берутся фильтром `GET /api/v1/stocks?ticker=<ticker>`
  (case-insensitive exact match, без нового backend-эндпоинта); позиция — через
  Portfolio API. После сделки trading-мутации инвалидируют
  `account`/`stocks`/`portfolio`/`transactions`, поэтому Dashboard, Portfolio,
  Transactions и Stock Detail обновляются автоматически (каталог `stocks` теперь
  инвалидируется prefix-ключом, что захватывает и detail-запрос по тикеру).
  Loading — Skeleton, при ненайденной акции — Error State «Stock not found.» с
  кнопкой возврата в каталог. Кнопка Back to Stocks возвращает в каталог.
  **Price Chart** (`StockPriceChart.tsx`, монтируется с `key={ticker}`, чтобы графики
  разных акций были полностью независимы): **линейный** график (Area) close price поверх
  **Lightweight Charts**, данные — `GET /api/v1/stocks/{ticker}/history?period=&from=`
  (backend сам идёт в MOEX ISS Candles и возвращает OHLCV; фронт рисует только `close`).
  Тонкая обёртка над обобщённым графиковым примитивом `shared/components/charts/PriceLineChart`
  (переиспользуется и графиком стоимости портфеля); вся визуальная часть, crosshair,
  зум и lazy-load живут в примитиве и его хелперах `shared/lib/chart/`
  (`chartTheme.ts`, `chartZoom.ts`, `periods.ts`).
  Линия цвета `#3dba8d` с прозрачным градиентом к оси X, чистый фон без сетки. Модель
  «кнопка = интервал свечи» (стиль Т-Инвестиций / MOEX): **1D / 1W / 1M / 1Y** —
  дневные/недельные/месячные/квартальные свечи со стартовой глубиной 3 мес / 5 мес /
  3 года / 10 лет; активная кнопка подсвечена фирменным цветом `#3dba8d`. **Колесо
  мыши масштабирует логическую шкалу времени относительно точки под курсором** (zoom
  in/out, встроенный `handleScale.mouseWheel`) — как в торговых терминалах; точка под
  курсором остаётся визуальным центром масштаба, страница вне графика скроллится как
  обычно. **Перетаскивание ЛКМ** панорамирует и при выходе за левый край догружает
  более старые данные **тем же интервалом** — серия всегда одного bucket size, без
  артефактов. Правый край прибит к последней свече (`fixRightEdge`) — в будущее двигать
  нельзя. **Crosshair-интерфейс при наведении** — собственный DOM-overlay
  (`pointerEvents: 'none'`, не влияет на canvas), управляемый из одного обработчика
  `mousemove` без re-render React: вертикальная **чёрная пунктирная направляющая**
  (`1px dashed #000`) через весь холст по X точки данных, **круглый маркер** в точке
  пересечения линии графика и направляющей (фирменный `#3dba8d` + мягкое свечение,
  плавная CSS-анимация появления/исчезновения по классу), и **tooltip** с датой и ценой
  на фиксированном смещении справа-снизу от курсора. Встроенный crosshair-маркер серии
  отключён (`crosshairMarkerVisible: false`), чтобы не было второй «точки». При зуме
  (мышь неподвижна, `mousemove` не стреляет) overlay перепозиционируется по последней
  позиции курсора в обработчике `subscribeVisibleLogicalRangeChange`. **Lazy-подгрузка**:
  триггерится pan'ом за левый край и фоновой предзагрузкой; ведёт **leading-edge
  throttle** (220 мс) — запрос уходит на первом импульсе, а не ждёт тишины (как при
  debounce); при сильном отдалении чанки подгружаются **циклом автодогрузки** (до 4 за
  заход) без ожидания новых событий. Предохранители против бесконечных запросов:
  `loadingRef`, `oldestAvailableDate` (пустой ответ запрещает дальнейший скролл влево),
  `needsLazyLoad`, `from >= loadedFrom`. График автоматически масштабируется (`autoSize`
  через внутренний `ResizeObserver`). Состояния Loading (Skeleton), Error («Не удалось
  загрузить историю цены» + Retry), Empty («Нет данных за период») переиспользуют общие
  компоненты. История не персистится и кэшируется React Query (ключ
  `queryKeys.stockHistory(ticker, period, from)`).
- **Trading**: покупка и продажа акций **в лотах**
  (`POST /api/v1/trades/buy|sell`, тело `{ stockId, lots }`) через Material UI
  Dialog с валидацией числа лотов (Zod). Под полем отображается подсказка
  `1 lot = N shares`, а при вводе — итоговое число акций («You are buying/selling:
  N shares») и estimated total. **В диалоге покупки (`BuyStockDialog`) под
  итоговой суммой показывается текущий баланс и баланс после покупки
  (`balance − total`, подсвечивается `error.main` при уходе в минус)** — значение
  информационное, фактическую проверку достаточности средств выполняет backend.
  Баланс берётся из `useAccountQuery` (тот же query-ключ, что в топбаре и
  дашборде). Backend сам вычисляет `quantity = lots × lotSize`;
  фронт не делает финансовых вычислений. После сделки React Query автоматически
  инвалидирует `account`/`stocks`/`portfolio`/`transactions`, поэтому Dashboard и
  каталог обновляются без перезагрузки. Успех/ошибка — тосты (Sonner); текст
  ошибки приходит из backend.
- **Portfolio**: позиции пользователя (`GET /api/v1/portfolio`) в Material UI
  Table — тикер, компания, количество (с подписью «(N lots)» при известном
  lotSize), средняя цена покупки, текущая цена, стоимость позиции, PnL (с
  цветовой индикацией) и кнопка Sell в каждой строке. Все финансовые показатели
  берутся из backend (PnL уже рассчитан, не пересчитывается на фронте). Кнопка
  Sell переиспользует существующий диалог продажи из Trading; после сделки
  портфель и Dashboard обновляются автоматически через `invalidateQueries`.
  Реализованы Skeleton при загрузке, блок ошибки с Retry и empty-state.
  **График стоимости портфеля** (`features/portfolio/ui/PortfolioValueChart.tsx`)
  — над таблицей позиций, в `Card`. Рисует изменение рыночной стоимости активов
  во времени (`GET /api/v1/portfolio/history?period=`) на Lightweight Charts:
  переиспользует обобщённый графиковый примитив `shared/components/charts/PriceLineChart`
  (тот же, что и график цены акции) с тем же стилем (линия `#3dba8d` + градиент,
  чистый фон без сетки, кнопки периода 1D/1W/1M/1Y, crosshair/tooltip/marker).
  В отличие от графика цены, `lazyLoad=false` — backend отдаёт готовый ряд на
  каждый период, догрузки «в прошлое» нет. Tooltip показывает дату и стоимость
  (`formatMoney`). Ключ React Query `queryKeys.portfolioHistory(period)` вложен в
  namespace `portfolio`, поэтому инвалидация `queryKeys.portfolio` в buy/sell
  мутациях обновляет и график после сделки. **Пустой портфель** — не пустой
  график, а Empty State с описанием и кнопкой «Перейти к акциям» (→ `/stocks`),
  реализован инлайн в `pages/PortfolioPage.tsx`. Бизнес-логика расчёта — только
  на backend; фронт только отображает готовый ряд.
- **Transactions**: история торговых операций (`GET /api/v1/transactions`) в
  Material UI Table — дата, тип (BUY/SELL с цветовой индикацией), тикер,
  количество, цена, сумма сделки. `GET /transactions` возвращает Spring Page
  (по умолчанию size=20, DESC по `createdAt`); фронт читает `content`.
  Read-only фича без фильтров/сортировки/UI-пагинации. После сделки
  trading-мутации уже инвалидируют `queryKeys.transactions`, поэтому новые
  записи появляются автоматически. Реализованы Skeleton при загрузке, блок
  ошибки с Retry и empty-state.
- **Onboarding**: приветственное окно при первом визите (`features/onboarding/ui/WelcomeDialog.tsx`)
  — MUI Dialog с описанием проекта, блоком стартового капитала (5 000 при первом
  посещении + 20 000 после регистрации), кнопкой «Начать» и ссылкой «Подробнее о
  проекте» → `/about`. Полностью независимый компонент: монтируется в `AppLayout`,
  сам управляет видимостью через флаг `imitrade_onboarding_completed` в localStorage
  (через централизованную утилиту `storage`). Показывается один раз; очистка
  localStorage возвращает окно. Ссылка на About живёт иконкой `Info` в шапке у бренда.
- **About**: отдельная страница `/about` (`pages/AboutPage.tsx`) с описанием проекта:
  что такое ImiTrade, возможности, источник данных (MOEX ISS API), используемые
  технологии (Backend / Frontend / Интеграции), архитектура, назначение. Контент
  разбит на отдельные MUI `Card` в адаптивной CSS-grid (1 колонка на mobile, 2 на
  десктопе). Статичная, без обращений к backend.
- **Адаптивный Header**: топбар (`widgets/topbar/`) разбит на подкомпоненты:
  `DesktopNav` (центральная навигация md+), `MobileNav` (бургер + левый Drawer
  для xs/sm), `UserMenu` (единое меню аккаунта/гостя вместо разрозненных кнопок).
  Переключение desktop/mobile — через MUI breakpoints (`sx.display: { xs, md }`),
  без `useMediaQuery`. `NAV_ITEMS` вынесены в общий модуль `navItems.tsx`.
  `UserMenu`: Guest → меню с «Регистрация (+20 000 ᛔ)» и «Вход»; Auth → меню с
  «Выйти» (через существующий `useLogout`, auth-логика не дублируется).
  Блок «Активы» виден всегда (на mobile компактнее — без подписи).

### Единые стандарты отображения и UX

Фронт приведён к консистентному виду во всех фичах:

- **Форматирование** централизовано в `shared/utils/format.ts`: деньги — всегда
  `formatMoney` (группировка пробелом, точка-разделитель, в конце — символ
  вымышленной валюты **«Дубли»** `ᛔ`), даты — всегда `formatDateTime`
  (`yyyy-mm-dd hh:mm`), PnL — всегда `formatProfitLoss` (знак `+`/`−` + цвет по
  теме MUI), относительное изменение цены — `formatPercent` (знак `+`/`−` + `%`,
  цвет по теме MUI). Локальные копии и ручное форматирование (`.toFixed`,
  `toLocaleString`, `Intl`) в фичах отсутствуют.
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

Отдельная страница `Account` убрана (сводка по аккаунту перенесена на Dashboard
через `AccountSummary` + `DashboardPortfolioPanel`, а краткая стоимость активов
постоянно видна в топбаре). `Login` и `Register` реализованы полностью (формы с
валидацией, мутациями и переходом на Dashboard после успеха). Рабочие маршруты:
`/dashboard`, `/stocks`, `/stocks/:ticker`, `/portfolio`, `/transactions`,
`/about`, `/login`, `/register`.

### Архитектура фронтенда (обновлено)

Feature-based: `app/` (роутер + провайдеры), `pages/` (тонкие страницы),
`widgets/` (layout/topbar), `features/<feature>/{api,model,types,ui,lib}`,
`shared/` (`api/`, `components/`, `hooks/`, `lib/`, `providers/`, `styles/`,
`types/`, `utils/`). Каждая фича — закрытый модуль; общение между фичами — только
через `shared/`. Псевдоним `@/` → `src/`. Сайдбара нет — вся навигация живёт в
топбаре (полноширинный `AppBar`), который также показывает суммарную стоимость
активов (`balance + portfolioValue`) и блок аутентификации.

Топбар (`widgets/topbar/`) разбит на подкомпоненты для адаптивности:
`AppTopBar` (композитор) + `DesktopNav` (центральная навигация md+) + `MobileNav`
(бургер + Drawer для xs/sm) + `UserMenu` (единое меню аккаунта/гостя) +
`navItems.tsx` (общий массив пунктов). Переключение desktop/mobile — через
breakpoints MUI (`sx.display`), без `useMediaQuery`. Онбординг живёт отдельно в
`features/onboarding/` и монтируется в `AppLayout`.

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
| `GET` | `/api/v1/portfolio/history` | JWT или Guest | История стоимости портфеля: `period` (1D/1W/1M/1Y, по умолчанию 1D) задаёт интервал свечи и глубину; опциональный `from` (ISO `yyyy-MM-dd`) сдвигает левую границу в прошлое. Backend реконструирует временной ряд рыночной стоимости (`Σ quantity_held × close`) replay'ем транзакций против исторических цен MOEX; не персистируется. Пустой список для пользователя без сделок. |
| `GET` | `/api/v1/stocks` | JWT или Guest | Каталог акций (пагинация, фильтры) |
| `GET` | `/api/v1/stocks/{id}` | JWT или Guest | Акция по id |
| `GET` | `/api/v1/stocks/{ticker}/history` | JWT или Guest | История цены по тикеру: `period` (1D/1W/1M/1Y, по умолчанию 1D) задаёт интервал свечи (день/неделя/месяц/квартал) и стартовую глубину (3мес/5мес/3года/10лет); `from` (ISO `yyyy-MM-dd`) — сдвиг левой границы в прошлое для lazy scroll тем же интервалом. Возвращает OHLCV-свечи, источник — MOEX ISS, не персистится. Фронтендовый линейный график рисует только `close`. |
| `POST` | `/api/v1/trades/buy` | JWT или Guest | Покупка акций |
| `POST` | `/api/v1/trades/sell` | JWT или Guest | Продажа акций |
| `GET` | `/api/v1/transactions` | JWT или Guest | История операций (фильтры) |
| `GET` | `/logos/{ticker}.svg` | публичный | SVG-логотип компании по тикеру (статический ресурс; `default.svg` — запасной) |

## MOEX integration

Актуальные цены и размеры лотов получаются из MOEX ISS API (публичный marketdata,
без аутентификации):

- `MoexClient.getMarketSnapshot` запрашивает для тикера последнее значение цены
  (`LAST`, блок `marketdata`) и размер лота (`LOTSIZE`, блок `securities`) в
  одном HTTP-вызове (`iss.only=marketdata,securities`). Поле `LOTSIZE` живёт в
  блоке `securities`, а не в `marketdata`. Оба значения читаются **строго с
  основного борда `TQBR`** (main T+ режим Мосбиржи), без fallback на другие
  борды: MOEX отдаёт по одной строке на каждый режим торгов, и значения там
  различаются (например, для GAZP лотность на `SMAL` = 1, на `TQBR` = 10).
  Используемый борд настраивается через `app.market.moex.board-id`.
- `MarketDataScheduler` по расписанию `@Scheduled` (по умолчанию каждые 60 c)
  обходит все акции и обновляет `current_price` (через
  `StockRepository.updateCurrentPrice`) и, при наличии значения от MOEX,
  `lot_size` (через `StockRepository.updateLotSize`) в таблице `stocks`. Если
  MOEX не вернул размер лота, сохранённое в БД значение не меняется.
- Бизнес-модули (`TradeService`, `AccountService`) не обращаются к MOEX напрямую —
  они читают `current_price` и `lot_size` из БД, поэтому всегда работают с
  последними сохранёнными планировщиком значениями.
- **История цены (candles)**: `MoexHistoryClient` (в `stocks/integration/moex/`)
  запрашивает `GET /engines/stock/markets/shares/securities/{ticker}/candles.json`
  с `iss.only=candles` и фиксированным набором колонок
  `begin,end,open,close,high,low,value,volume`. `StockHistoryService` вычисляет
  диапазон дат и интервал (`interval`) по периоду (`HistoryPeriod`: 1D→день
  (`interval=24`, лукбэк 3 мес), 1W→неделя (`7`, 5 мес), 1M→месяц (`31`, 3 года),
  1Y→квартал (`4`, 10 лет)), `MoexHistoryMapper` преобразует строки в `CandleResponse`
  (MSK→UTC), сортирует по времени и выбрасывает пустые бакеты. История **не
  персистится** — MOEX единственный источник; фронтенд обращается только к backend
  (`GET /api/v1/stocks/{ticker}/history`), к MOEX напрямую не идёт. Сетевые ошибки и
  ошибки MOEX маппятся в `MarketDataUnavailableException` (503).
- **Переиспользование истории цены для портфеля**: `PortfolioService.getHistory`
  реконструирует временной ряд стоимости портфеля (`GET /api/v1/portfolio/history`),
  вызывая тот же `StockHistoryService.getHistory(ticker, period, from)` — второй
  MOEX-клиент не создаётся. Внутри одного расчёта история каждого тикера
  запрашивается ровно один раз (локальный кэш по тикеру), что исключает N×M вызовов
  при многих позициях. Past-позиции восстанавливаются replay'ем транзакций
  (`TransactionRepository.findByUserIdOrderByCreatedAtAsc`); стоимость точки =
  `Σ quantity_held(stock, t) × close(stock, t)` (close — forward-fill последней
  свечи ≤ t). Точки, где пользователь ничего не держал (до первой покупки / после
  полной продажи), пропускаются — плоский нулевой отрезок не рисуется. Как и у
  истории цены, результат **не персистится** и считается in-memory; MOEX-ошибки
  тем же путём дают 503.

Параметры интеграции настраиваются в `application.yaml`
(`app.market.*`, см. раздел [Configuration](#configuration)).

## Guest Mode

Приложение поддерживает два типа пользователей: **Registered** и **Guest**.
Бизнес-логика не зависит от типа: торговля, портфель, транзакции и аккаунт
работают одинаково.

### Создание гостя

`POST /api/v1/guest` — публичный endpoint. Создаёт пользователя с
`is_guest = true`, случайным `guest_token` (UUID) и балансом **5 000**.

```json
{
  "guestToken": "8c33bb2e-8d4b-4e0c-b57d-0dce5f6c4e3f",
  "balance": 5000
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
- Баланс увеличивается на **20 000** (бонус за регистрацию)
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
