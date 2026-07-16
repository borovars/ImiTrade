# ImiTrade Frontend — Архитектура и правила

Frontend-клиент ImiTrade. SPA на React, общается с backend из корня репозитория
(`/api/v1/...`, см. корневой `readme.md`). Живёт в каталоге `frontend/`.

## Стек

- React 19 + TypeScript (strict)
- Vite 6
- React Router 7
- Axios
- TanStack Query 5
- Material UI (MUI) 7 + Emotion
- Lightweight Charts 5 (график цены акции: `createChart` + `addSeries(AreaSeries, …)`)
- React Hook Form 7 + Zod 3 (`@hookform/resolvers`)
- Sonner (тосты)
- Lucide React (иконки — **не** `@mui/icons-material`)

## Запуск

```bash
cd frontend
npm install
npm run dev       # Vite dev-сервер, по умолчанию http://localhost:5173
npm run build     # tsc + vite build
npm run preview   # превью production-сборки
npm run lint      # eslint
npm run format    # prettier
```

Базовый URL backend берётся из `VITE_API_URL` (по умолчанию `http://localhost:8080`).
Задаётся в `frontend/.env`. Backend должен быть запущен (`./gradlew bootRun` из корня).

Путь-алиас `@/` → `src/` настроен и в `tsconfig.json`, и в `vite.config.ts`. Все
импорты — через `@/...`.

## Архитектура

Используется **feature-based architecture**.

```
src/
├── main.tsx                # entry: <App/> в StrictMode
├── app/                    # Инициализация приложения
│   ├── App.tsx             # композиция провайдеров
│   ├── AppRouter.tsx       # маршруты
│   └── providers/          # AuthProvider, queryProvider
├── pages/                  # Страницы, привязанные к маршрутам (тонкие)
├── widgets/                # Самостоятельные блоки UI (layout, topbar)
├── features/               # Изолированные бизнес-сущности
│   └── <feature>/
│       ├── api/            # функции-обёртки над apiClient
│       ├── model/          # хуки React Query (use*Query, use*Mutation)
│       ├── types/          # типы запросов/ответов фичи
│       ├── ui/             # React-компоненты фичи
│       └── lib/            # утилиты и валидаторы фичи (zod-схемы и т.д.)
└── shared/                 # Переиспользуемый код, не привязанный к бизнес-логике
    ├── api/                # apiClient (Axios), endpoints, общие типы ответов
    ├── lib/                # queryKeys, apiError, storage
    ├── providers/          # MuiThemeProvider, ToastProvider
    ├── styles/             # MUI-тема
    ├── utils/              # чистые функции (formatMoney и т.д.)
    ├── components/         # переиспользуемые UI-компоненты состояний (см. ниже)
    ├── hooks/              # (зарезервировано) общие хуки
    └── types/              # (зарезервировано) глобальные типы
```

## shared/components — UI-состояния

Единые компоненты для loading/error/empty во всех фичах. **Не дублировать** эти
блоки inline в страницах — всегда импортировать отсюда (`@/shared/components`):

- **`StateError`** — блок ошибки: иконка `AlertCircle`, заголовок (проп `title`),
  текст `error?.message` (fallback `helperText`) и кнопка `Retry` → `onRetry`.
  Цвет иконки — `error.main` из темы MUI (жёстко hex не прописывается).
  Пропсы: `{ title, helperText?, error?, onRetry, retryText? }`.
- **`StateEmpty`** — empty-state: заголовок (`title`) + опциональный
  `helperText`. Пропсы: `{ title, helperText? }`.
- **`TableSkeleton`** — скелетон табличных данных: строка-шапка (56px) + N строк
  тела (48px), по умолчанию `rows = 3`. Пропсы: `{ rows? }`.

Потребители: `StocksPage`, `PortfolioPage`, `TransactionsPage` (loading/error/empty
таблиц) и `AccountSummary` (error/empty карточек; loading там свой — 4×Skeleton 140,
т.к. структура карточек отличается от таблицы). `TableSkeleton` и `StateError`
также переиспользуются внутри `shared/components/charts/PriceLineChart.tsx` для
loading/error-состояний графика (overlay поверх canvas, чтобы контейнер графика
оставался в DOM — `createChart` не должен попадать в цикл ожидания).

> Внутренние папки фичи — **`api/model/types/ui`** (+ опционально `lib`).
> В коде используется именно такое деление, а не `components/`/`hooks/` из ранних
> набросков. React Query-хуки живут в `model/`, презентационные компоненты — в `ui/`.

## Правила размещения компонентов

1. **Компонент используется только внутри одной фичи** — `features/<feature>/ui/`.
2. **Компонент нужен в двух и более фичах** — поднимается в `shared/components/`.
3. **Страницы** живут в `pages/` и не содержат бизнес-логики — только компоновку.
4. **Widgets** — сложные UI-блоки (шапка, сайдбар), могут объединять несколько фич.

## Провайдеры

Композиция в `app/App.tsx` (снаружи внутрь):

```
AuthProvider → QueryProvider → MuiThemeProvider → ToastProvider → AppRouter
```

- **AuthProvider** (`app/providers/AuthProvider.tsx`) — оборачивает `AuthStateProvider`
  из `features/auth` и запускает `bootstrapAuth` один раз (показывает `null`, пока
  `state.isReady === false`, чтобы избежать мигания гостевого/авторизованного UI).
- **QueryProvider** — `QueryClientProvider`, дефолты: `staleTime: 5 мин`,
  `refetchOnWindowFocus: false`, `retry: 1`. `QueryClient` создаётся один раз.
- **MuiThemeProvider** — `ThemeProvider` + `<CssBaseline/>`.
- **ToastProvider** — `<Toaster position="top-right" richColors />` из sonner.

## Роутинг

`app/AppRouter.tsx` (React Router 7, `BrowserRouter`):

- `/` → редирект на `/dashboard`
- `/login`, `/register` — отдельные страницы (вне layout)
- `<AppLayout/>` (layout-маршрут с `<Outlet/>`) оборачивает защищённые страницы:
  `/dashboard`, `/stocks`, `/stocks/:ticker`, `/portfolio`, `/transactions`
  (отдельный маршрут `/account` убран — сводка по аккаунту перенесена на Dashboard,
  краткая стоимость активов постоянно видна в топбаре)

`AppLayout` (`widgets/layout/`) — фиксированный `AppBar` (топбар) на всю ширину,
сайдбара нет. Навигация (Главная / Акции / Портфель / Операции) живёт в топбаре
по центру через `NavLink` с подсветкой активного раздела; `<Toolbar/>` резервирует
место под фиксированный AppBar.

## Аутентификация (Auth)

Feature `features/auth/` полностью реализована (JWT + Guest, bootstrap, login,
register, logout). Структура:

```
features/auth/
├── api/      # authApi.ts — login(), register(), getCurrentUser()
├── lib/      # loginSchema.ts, registerSchema.ts (zod)
├── model/    # authStore.tsx, authService.ts, useLoginMutation.ts,
│            # useRegisterMutation.ts, useLogout.ts, types.ts
├── types/    # authTypes.ts — DTO под backend-контракт
└── ui/       # LoginForm.tsx, RegisterForm.tsx
```

State-слой — `features/auth/model/` поверх `useReducer` + React Context:

- **`authStore.tsx`** — `authReducer`, `AuthContext`, хуки `useAuth()`,
  `useAuthState()`, `useAuthDispatch()`. Действия: `SET_GUEST | SET_AUTH | RESET | SET_READY`.
- **`authService.ts`** — `bootstrapAuth`, `initGuestIfNeeded`, `saveJwtToken`,
  `saveGuestToken`, `resetAuth`. **Bootstrap тянет реальный профиль**: если есть
  JWT — `GET /api/v1/users/me` → `SET_AUTH` с настоящим пользователем (при
  протухшем/невалидном JWT токен отбрасывается и приложение проваливается в
  guest-режим); иначе если есть guest-token — `SET_GUEST`; иначе автоматически
  создаётся гость.
- **`types.ts`** — `UserType = 'guest' | 'auth'`, `User` (повторяет
  `CurrentUserResponse`: `id`, `email`, `username`, `balance`, `createdAt`),
  `AuthState`.

API-слой (`features/auth/api/authApi.ts`) — типизированные обёртки над `apiClient`
по образцу `features/account/api/accountApi.ts`:

- `login(data)` → `POST /api/v1/auth/login` → `AuthResponse { token, type, expires_in }`;
- `register(data)` → `POST /api/v1/auth/register` (с опциональным `guestToken` для
  конвертации гостя) → `AuthResponse`;
- `getCurrentUser()` → `GET /api/v1/users/me` → `CurrentUserResponse`.

Мутации (`features/auth/model/use*`), по образцу `useBuyStockMutation`:

- **`useLoginMutation`** — `mutationFn`: `login` → `storage.setJwtToken` →
  `getCurrentUser` (с полученным JWT); `onSuccess`: `saveJwtToken` + инвалидация
  `account`/`portfolio`/`transactions` (данные пользователя нужно перетянуть с
  backend); тост `Welcome back, ${username}`. `onError` — тост, форма не очищается.
- **`useRegisterMutation`** — то же, но в `register` автоматически прокидывается
  текущий `storage.getGuestToken()` (конвертация гостя с сохранением баланса,
  портфеля и истории; backend начисляет +20 000); после успеха guest-токен
  удаляется (он недействителен после конвертации).
- **`useLogout`** — frontend-only выход: `storage.clearAuth` → `RESET` →
  `window.location.assign('/dashboard')`. Полная перезагрузка на `/dashboard`;
  при старте `bootstrapAuth` создаст нового гостя, React Query cache будет чист
  (новая загрузка = новое состояние), и пользователь окажется в гостевом режиме.

Формы (`features/auth/ui/`) — RHF + `zodResolver` (образец `TradeStockDialog`):

- **`LoginForm`** — Email, Password, кнопки Sign In / Back + ссылка на регистрацию.
- **`RegisterForm`** — Username, Email, Password, Confirm Password, кнопки
  Create Account / Back + ссылка на вход. Zod-схема зеркалит backend-валидацию
  `RegisterRequest` (username 3–100 + `^[A-Za-z0-9_.-]+$`, password 8–100,
  password === confirmPassword).

Страницы `pages/LoginPage.tsx` и `pages/RegisterPage.tsx` — тонкие обёртки над
формами в `<Container maxWidth="xs">` + `<Paper>`. При `userType === 'auth'`
редиректят на `/dashboard`.

Топбар (`widgets/topbar/AppTopBar.tsx`) — единый центр навигации и сводки (сайдбара
нет): бренд «ImiTrade» слева, основная навигация (Главная / Акции / Портфель /
Операции) по центру (абсолютное центрирование через `position:absolute;
left:50%`, текущий раздел подсвечен `.active`-классом `NavLink`), блок
аутентификации справа. Перед блоком аутентификации — **краткая сводка активов**:
`totalAssets = balance + portfolioValue` (DTO не содержит этого поля, считается
на фронте из двух примитивов), обновляется через `useAccountQuery` (тот же
query-ключ, что и на Dashboard — запрос дедуплицируется React Query).
Навигация и сводка показываются всегда; блок аутентификации зависит от режима:
гость → «Войти» + «Регистрация» (ссылки на `/login`, `/register`);
залогинен → username (из `state.user`) + кнопка «Выйти» (`useLogout`, без reload).
Чип «Пользователь»/«Гость» остаётся. Навигация и кнопки локализованы на русский.

Токены хранятся в `localStorage` через `shared/lib/storage.ts` (ключи
`imitrade_jwt_token`, `imitrade_guest_token`). JWT имеет приоритет над guest-token.

> **Важно про 401-интерсептор**: в `shared/api/apiClient.ts` ветка 401 теперь
> исключает не только создание гостя (`/guest`), но и `/auth/login` +
> `/auth/register` — иначе неудачный логин гостя (неверный пароль → 401) чистил
> бы гостевую сессию и перезагружал страницу, ломая UX формы. На этих
> эндпоинтах `ApiError` просто прокидывается, мутация показывает его тостом.

## shared/api — HTTP-слой

- **`apiClient.ts`** — экземпляр Axios (`baseURL = VITE_API_URL`, `Content-Type: application/json`).
  - **Request-интерсептор** автоматически подставляет `Authorization: Bearer <jwt>`
    (приоритет) или `X-Guest-Token <guest>`.
  - **Response-интерсептор** нормализует ошибку через `normalizeApiError` в `ApiError`
    (`message`, `status`) и избирательно тостит: 401 — чистит auth и редиректит на `/`
    (только если ранее был токен, и **не** для запроса создания гостя `/guest` **и не
    для `/auth/login` / `/auth/register`** — иначе неудачный логин гостя выкинул бы
    его сессию); 403 — тост «Доступ запрещён»; ≥500 — тост «Ошибка сервера».
    **4xx (400/404/409) НЕ тостятся автоматически** — их показывает вызывающий код
    (например, `onError` мутации).
  - Также экспортирует хелпер `createGuest()` (POST `/api/v1/guest`) и реэкспорт `ApiError`.
- **`endpoints.ts`** — `API_ENDPOINTS` (все пути `/api/v1/...` в одном месте, включая
  `AUTH.LOGIN`/`AUTH.REGISTER`, `USERS.ME`, `TRADING.BUY`/`TRADING.SELL`,
  `STOCKS.history(ticker)` → `/api/v1/stocks/{ticker}/history`). Новые пути
  добавлять сюда, **не** дублировать строками.
- **`types.ts`** — общие типы ответов (`GuestResponse`, `ApiErrorPayload`).

Конвенция API-функции фичи (по образцу `features/account/api/accountApi.ts`):

```ts
export async function getAccount(): Promise<AccountResponse> {
  const response = await apiClient.get<AccountResponse>(API_ENDPOINTS.ACCOUNT.BASE);
  return response.data; // разворачиваем axios-ответ, отдаём типизированный payload
}
```

## TanStack Query

- **Хуки** живут в `features/<feature>/model/`. Query-хук возвращает `useQuery(...)`,
  mutation-хук — `useMutation(...)`; страница/компонент деструктурирует
  `{ data, isLoading, isError, error, refetch }` / `{ mutate, isPending }`.
- **Query-ключи** — единый реестр `shared/lib/queryKeys.ts`:

  ```ts
  export const queryKeys = {
    account: ['account'] as const,
    stocks: ['stocks'] as const,
    stockDetails: (ticker: string) => ['stocks', 'detail', ticker] as const,
    stockHistory: (ticker: string, period: string, fromIso: string) =>
      ['stocks', 'history', ticker, period, fromIso] as const,
    portfolio: ['portfolio'] as const,
    portfolioHistory: (period: string) => ['portfolio', 'history', period] as const,
    transactions: ['transactions'] as const,
  };
  ```

  `portfolioHistory` вложен в namespace `portfolio`, поэтому инвалидация
  `queryKeys.portfolio` (prefix) в buy/sell-мутациях захватывает и историю
  стоимости портфеля — после сделки график обновляется автоматически.

  Это **канал взаимодействия между фичами**: мутация фичи trading инвалидирует
  чужие ключи через `queryKeys.account` и т.д., а не через внутренние константы
  других фич. **Строковые литералы ключей не используем** — только `queryKeys.*`.
- **Mutation + инвалидация** (образец — `features/trading/model/useBuyStockMutation.ts`):
  в `onSuccess` вызываем `queryClient.invalidateQueries({ queryKey: queryKeys.account })`
  для затронутых сущностей (`account`/`stocks`/`portfolio`/`transactions`) и
  `toast.success(...)`; в `onError` — `toast.error(error.message)`.
  Ручное обновление состояния (`setQueryData`) **не используем** (optimistic updates запрещены).
  `stocks` инвалидируется prefix-ключом, чтобы захватить и detail-запрос по тикеру
  (`queryKeys.stockDetails(ticker)`).

## Обработка ошибок

Трёхуровневая, без собственного глобального error boundary:

1. **`ApiError`** (`shared/lib/apiError.ts`) — класс с `.message` и `.status`;
   `normalizeApiError(error)` достаёт `response.data.message` из axios-ошибки.
   Все промисы из `apiClient` реджектятся **`ApiError`**, а не `AxiosError`.
2. **Авто-тосты** интерсептора для 401/403/≥500 (см. выше).
3. **Per-компонентный UI**: каждый компонент деструктурирует `isError`/`error` и
   рендерит единый блок ошибки (иконка `AlertCircle` из lucide, заголовок, текст
   `error?.message`, кнопка Retry → `refetch()`). Для mutation-ошибок (4xx) —
   `toast.error` в `onError` хука.

## Feature: trading (полный торговый цикл)

Единственная write-фича на данный момент. Структура:

- `types/tradeTypes.ts` — `BuyStockRequest`, `SellStockRequest`, `TradeResponse`
  (по контракту backend: `{ stockId, lots }`; `TradeResponse` —
  `{ transactionId, stockTicker, type, quantity, price, totalAmount, lotSize, lots }`).
  Торговля идёт **в лотах**: фронт отправляет `lots`, backend вычисляет
  `quantity = lots × lotSize` и возвращает фактическое число акций.
- `api/tradingApi.ts` — `buyStock()`, `sellStock()` через `apiClient.post`.
- `lib/tradeSchema.ts` — Zod-валидация числа лотов (`lotsSchema`, поле `lots`,
  `coerce.number().int().positive()`). Кратность `lotSize` не валидируется —
  её гарантирует backend.
- `model/useBuyStockMutation.ts`, `model/useSellStockMutation.ts` — `useMutation` с
  инвалидацией `account`/`stocks`/`portfolio`/`transactions` (stocks — prefix-ключом,
  чтобы обновить и каталог, и Stock Detail по тикеру) и тостами (`Bought N lots
  (M shares) of TICKER`).
- `ui/TradeStockDialog.tsx` — общая форма сделки (MUI `Dialog` + RHF + `zodResolver`),
  переиспользуется покупкой и продажей. Поле «Lots» с подсказкой
  `1 lot = N shares`, live-расчёт «You are buying/selling: M shares» и estimated
  total (`shares × currentPrice`). `ui/BuyStockDialog.tsx`, `ui/SellStockDialog.tsx`
  — тонкие обёртки, **без дублирования** логики.

Кнопки Buy/Sell выводятся в таблице акций (`features/stocks/ui/StocksTable.tsx`,
с колонкой Lot Size); диалоги монтируются там же с локальным состоянием. После
успешной сделки диалог закрывается, тост успеха показывается в `onSuccess` хука.

## Feature: stocks — sparkline (мини-график в каталоге)

В каталоге акций каждая строка содержит **мини-график цены** (sparkline) за
последний год — отдельная колонка рядом с ценой. Назначение: быстро оценить
тенденцию тикера без перехода на страницу детали. Структура:

- `types/sparklineTypes.ts` — `SparklinePoint` (`{ time, close }`). Локальный тип
  (а не импорт из `features/stock-details`) — фичи не импортируют внутренности
  друг друга.
- `api/stocksSparklineApi.ts` — `getStockSparkline(ticker, from)` ходит в тот же
  эндпоинт, что и большой график — `GET /api/v1/stocks/{ticker}/history`
  (`period=1M` = месячные свечи, `from` = сегодня − 365 дней). OHLCV-контракт
  отсекается на уровне API-функции, наружу идёт только `close`.
- `ui/StockSparkline.tsx` — чистый SVG (`<polyline>`), **без графических
  библиотек**: 10 строк в таблице не должны тянуть 10 canvas'ов lightweight-charts.
  Размер 88×32, цвет линии — по годовому тренду (зелёный если вырос, красный если
  упал), при пустых/loading/ошибочных данных ячейка остаётся пустой (sparkline
  декоративный, не должен мешать основной информации). Ключ React Query —
  `queryKeys.stockHistory(ticker, '1M', fromIso)`, т.е. общий кэш с большим
  графиком акции для тех же параметров (дедупликация запросов).

## Feature: account + DashboardPortfolioPanel

- `features/account/` — сводка по аккаунту: `api/accountApi.ts`
  (`getAccount()` → `GET /api/v1/account`), `model/useAccountQuery.ts`,
  `ui/AccountSummary.tsx`. `AccountSummary` рендерит карточки (баланс, стоимость
  портфеля, PnL, число позиций) с иконками lucide и info-подсказками (`Tooltip`);
  состояния loading/error/empty обрабатываются (`StateError`/`StateEmpty`,
  loading — собственный Skeleton 4×140, т.к. структура карточек отличается от
  таблицы). Монтируется на `DashboardPage`.
- `features/portfolio/ui/DashboardPortfolioPanel.tsx` — read-only панель позиций
  пользователя для главной (Dashboard). Показывает тикер, компания, количество,
  среднюю цену покупки, текущую цену, изменение цены (абсолют + процент к средней
  через `formatProfitLoss` + `formatPercent`) и PnL. **Без кнопок действий** —
  Dashboard остаётся сводным обзором; торговля ведётся со страницы «Портфель»
  (`PortfolioTable` + `SellStockDialog`). Данные — `usePortfolioQuery` (тот же
  query-ключ, что и на странице портфеля → запрос дедуплицируется). Состояния
  loading/error/empty — через `TableSkeleton`/`StateError`/`StateEmpty`.

## Feature: portfolio (только чтение)

Read-only фича позиций пользователя. Структура:

- `types/portfolioTypes.ts` — `PortfolioPosition` (по контракту backend:
  `{ stockId, ticker, companyName, quantity, averagePrice, currentPrice, pnl, lotSize }`).
  `GET /api/v1/portfolio` отдаёт **простой массив** (не Spring Page).
- `api/portfolioApi.ts` — `getPortfolio()` через `apiClient.get`.
- `model/usePortfolioQuery.ts` — `useQuery` с `queryKeys.portfolio`.
- `ui/PortfolioTable.tsx` — Material UI Table со столбцами Ticker, Company,
  Quantity (с подписью «(N lots)» при известном `lotSize`), Average Price,
  Current Price, Position Value, Profit/Loss, Actions. Адаптер `toStock()`
  прокидывает `lotSize` в `SellStockDialog`.
- **История стоимости портфеля** (график):
  - `types/portfolioHistoryTypes.ts` — `PortfolioHistoryPoint = HistoryPoint`
    (алиас на общий тип `shared/lib/chart/historyPoint.ts`, формат
    `{ time, value }`) + `PortfolioHistoryPointDto` (сырой DTO до нормализации
    чисел).
  - `api/portfolioHistoryApi.ts` — `getPortfolioHistory(period)` через
    `apiClient.get(API_ENDPOINTS.PORTFOLIO.HISTORY, { params: { period } })`.
    `from` не передаётся: график стоимости рисует один диапазон на период,
    lazy scroll-to-past не используется. Вся бизнес-логика расчёта
    (`Σ quantity_held × close`) — на backend; фронт только отображает готовый ряд.
  - `model/usePortfolioHistoryQuery.ts` — `useQuery` с ключом
    `queryKeys.portfolioHistory(period)`. Вложен в namespace `portfolio`, поэтому
    инвалидация `queryKeys.portfolio` (prefix) в trading-мутациях захватит и
    историю — после сделки график обновляется автоматически.
  - `ui/PortfolioValueChart.tsx` — тонкая обёртка над обобщённым
    `shared/components/charts/PriceLineChart.tsx`: `seriesKey='portfolio'`,
    `fetchData={getPortfolioHistory}`, `queryKey={queryKeys.portfolioHistory}`,
    `lazyLoad={false}` (backend отдаёт готовый ряд на каждый период), 
    `tooltipValueLabel='Стоимость'`. Вся визуальная часть, crosshair/tooltip/marker
    и переключатель периодов — переиспользуются из `PriceLineChart` (тот же, что и
    график цены акции).

Принципы фичи:

- **Финансовые показатели берутся из backend**: `pnl` уже рассчитан
  (`(currentPrice - averagePrice) * quantity`) и не пересчитывается на фронте.
  Единственное производное значение — Position Value = `quantity * currentPrice`
  (display-умножение из примитивов backend, не пересчёт агрегатов). История
  стоимости портфеля тоже считается на backend (replay транзакций против
  исторических цен MOEX) — фронт получает готовый временной ряд.
- **Profit/Loss** — цветовая индикация через тему MUI (зелёный/красный/нейтрально)
  общей функцией `formatProfitLoss` из `shared/utils/format.ts`. Цвета берутся из
  темы (`success.main` / `error.main` / `text.primary`), жёстко не прописываются.
- **Продажа** — кнопка Sell в каждой строке открывает переиспользованный
  `SellStockDialog` из `features/trading/ui/`. Новая логика продажи не пишется.
  Позиция портфеля адаптируется в объект формы `Stock` (`stockId` → `id`,
  `exchange` пустой — он не нужен для продажи).
- **Автообновление** — после успешной продажи существующие trading-мутации уже
  инвалидируют `queryKeys.portfolio`/`account`/`stocks`/`transactions`, поэтому
  таблица портфеля, график его стоимости и Dashboard обновляются без правок в
  trading (`queryKeys.portfolio` prefix захватывает и `portfolioHistory`).
- **Состояния** (loading/error/empty) обрабатываются на странице
  `pages/PortfolioPage.tsx`: Skeleton при загрузке позиций, блок ошибки с кнопкой
  Retry; над таблицей позиций (когда они есть) — `Card` с графиком стоимости
  портфеля (`PortfolioValueChart`, у которого собственный loading/error/empty
  overlay поверх canvas). **Пустой портфель** — не пустой график/таблица, а
  Empty State с описанием и кнопкой «Перейти к акциям» (→ `/stocks`, инлайн в
  `PortfolioPage`, не расширяет общий `StateEmpty`, у которого нет action-пропа).

## Feature: transactions (только чтение)

Read-only фича истории торговых операций. Структура:

- `types/transactionsTypes.ts` — `Transaction` (по контракту backend
  `TransactionResponse`: `{ id, type: 'BUY'|'SELL', ticker, quantity, price,
  totalAmount, createdAt }`) и `TransactionPage` (обёртка Spring Page:
  `{ content, totalElements, totalPages, size, number }`).
  `GET /api/v1/transactions` отдаёт **Spring Page** (по умолчанию size=20,
  DESC по `createdAt`), а не простой массив — этим отличается от portfolio.
- `api/transactionsApi.ts` — `getTransactions()` через `apiClient.get`,
  возвращает всю `TransactionPage` (метаданные пагинации сохраняются для
  будущей готовности, UI пока рендерит только `content`).
- `model/useTransactionsQuery.ts` — `useQuery` с `queryKeys.transactions`.
- `ui/TransactionsTable.tsx` — Material UI Table со столбцами Date, Type,
  Ticker, Quantity, Price, Total Amount. Чисто презентационный (без Actions).

Принципы фичи:

- **Read-only**: никаких действий/диалогов; данные только отображаются.
- **Type (BUY/SELL)** — цветовая индикация через тему MUI: BUY → `success.main`
  (зелёный), SELL → `error.main` (красный). Цвета берутся из темы, жёстко не
  прописываются.
- **Дата** — через общий хелпер `formatDateTime` из `shared/utils/format.ts`
  (детерминированный формат `yyyy-mm-dd hh:mm`, не браузерная локаль).
- **Деньги** (price, totalAmount) — через `formatMoney`.
- **Автообновление** — после успешной сделки существующие trading-мутации уже
  инвалидируют `queryKeys.transactions`, поэтому таблица обновляется без правок
  в trading.
- **Состояния** (loading/error/empty) обрабатываются на странице
  `pages/TransactionsPage.tsx`: Skeleton при загрузке, блок ошибки с кнопкой
  Retry, empty-state с приглашением начать торговать.

## Feature: stock-details (детальная страница акции)

Страница детальной информации об акции и торговых операций с неё. Маршрут:
`/stocks/:ticker` (например, `/stocks/SBER`). Структура:

- `api/stockDetailsApi.ts` — `getStockByTicker(ticker)` через `apiClient.get`
  к `GET /api/v1/stocks?ticker=<ticker>` (backend не имеет эндпоинта по
  тикеру — только по числовому id; используется существующий фильтр с
  case-insensitive exact match через `StockSpecifications.hasTickerIgnoreCase`,
  возвращающий Spring Page; из ответа берётся `content[0]`). Возвращает
  `Stock | null` — `null` означает «не найдено» (страница покажет Error State).
- `model/useStockDetailsQuery.ts` — `useQuery` с ключом
  `queryKeys.stockDetails(ticker)` (вкладывается в namespace `stocks`, поэтому
  инвалидация `queryKeys.stocks` prefix-ключом захватит и detail-запрос).
- `api/stockHistoryApi.ts` + `model/useStockHistoryQuery.ts` +
  `model/historyTypes.ts` — история цены для графика. `getStockHistory(ticker,
  from, period)` ходит в `GET /api/v1/stocks/{ticker}/history?period=&from=`
  (`period` = `1D`/`1W`/`1M`/`1Y`, `from` = ISO `yyyy-MM-dd` для lazy
  scroll-to-past). Backend возвращает полный OHLCV (`CandleResponse`), но
  API-слой **отсекает** лишнее и отдаёт наружу только `{ time, value }`
  (`HistoryPoint` из `shared/lib/chart/historyPoint.ts`; `value` = `close`
  свечи — общее имя поля, чтобы тот же тип потреблял и график стоимости
  портфеля) — свечной контракт не протекает в UI. `HistoryPeriodCode` и
  `HistoryPoint` реэкспортируются из `model/historyTypes.ts` для обратной
  совместимости, но канонически живут в `shared/lib/chart/`. Ключ React Query —
  `queryKeys.stockHistory(ticker, period, fromIso)`.
- `ui/StockPriceChart.tsx` — **тонкая обёртка** над обобщённым графиковым
  примитивом `shared/components/charts/PriceLineChart.tsx`: подставляет тикер
  (`seriesKey={ticker}`), источник данных (`getStockHistory`), ключ кэша
  (`queryKeys.stockHistory`) и `lazyLoad`. Вся реализация графика (создание,
  тема, crosshair/tooltip/marker, зум, lazy-load) перенесена в `shared/` и
  переиспользуется графиком стоимости портфеля — см. раздел
  [Обобщённый график (shared)](#обобщённый-график-shared) ниже.
- **Обобщённый график (shared)** — `shared/components/charts/PriceLineChart.tsx`
  + хелперы `shared/lib/chart/` (`chartTheme.ts`, `chartZoom.ts`, `periods.ts`,
  `historyPoint.ts`). `PriceLineChart` — профессиональный **линейный** график
  (Area, только `value`) на **Lightweight Charts** v5 (`createChart` +
  `chart.addSeries(AreaSeries, …)`). Props: `seriesKey` (ключ пере-монтирования и
  изоляции кэша — тикер для акции, `'portfolio'` для портфеля), `fetchData`
  (загрузчик точек, реализация скрыта в фиче), `queryKey` (фабрика ключа из
  `queryKeys`), `lazyLoad` (default `true` — колесо мыши + ЛКМ-панорама +
  фоновая предзагрузка старых данных тем же интервалом; `false` для портфеля),
  `tooltipValueLabel` («Цена» / «Стоимость»), `initialPeriod`. Цвет линии
  `#3dba8d`, прозрачный градиент под ней, чистый фон без сетки. **Модель «кнопка
  = интервал свечи»** (стиль Т-Инвестиций / MOEX): 1D=дневные/3мес,
  1W=недельные/5мес, 1M=месячные/3года, 1Y=квартальные/10лет
  (`PERIOD_LOOKBACK_DAYS`, `CANONICAL_PERIODS` в `chartZoom`); активная кнопка —
  фирменного цвета. **Колесо мыши = зум** относительно точки под курсором
  (встроенный `handleScale.mouseWheel=true`): масштабирует логическую шкалу
  времени так, что точка под курсором остаётся визуальным центром (как в
  торговых терминалах), страница вне графика при этом скроллится как обычно
  (библиотека сама делает `preventDefault` только внутри холста).
  `handleScroll.mouseWheel` оставлен `false` — колесо именно зумит, а не
  панорамирует. **Перетаскивание ЛКМ** (`handleScroll.pressedMouseMove=true`)
  панорамирует и при выходе за левый край догружает более старые данные **тем же
  интервалом** — серия всегда одного bucket size, без артефактов (пиков/провалов)
  — только при `lazyLoad=true`. Правый край прибит (`fixRightEdge`), левый НЕ
  прибит (`fixLeftEdge=false`) — выход за загруженные данные триггерит
  lazy-подгрузку (`subscribeVisibleLogicalRangeChange` → `chartZoom.needsLazyLoad`
  + `requestLazyLoad`). **Crosshair-интерфейс при наведении** — собственный
  DOM-overlay (`pointerEvents: 'none'`, не влияет на canvas), управляемый из
  одного обработчика `mousemove` без re-render React: вертикальная **чёрная
  пунктирная направляющая** (`vLineStyle`, `1px dashed #000`) через весь холст по
  X точки данных (`timeScale().timeToCoordinate`), **круглый маркер**
  (`markerStyle`, `#3dba8d` + мягкое свечение) в точке пересечения линии графика
  и направляющей (`series.priceToCoordinate(value)` для Y), и **tooltip** с
  датой и значением (`formatDateTime` + `formatMoney`, подпись значения —
  `tooltipValueLabel`) на фиксированном смещении справа-снизу от курсора
  (`positionTooltip`, `TOOLTIP_OFFSET_PX`). Появление/исчезновение маркера —
  плавная CSS-анимация (класс `.cct-marker-visible`, инъекция стилей в
  `ensureOverlayStyle`). Встроенный crosshair-маркер серии отключён
  (`crosshairMarkerVisible: false` в `areaSeriesOptions`), чтобы при зуме не
  появлялась вторая «точка». При зуме (мышь неподвижна, `mousemove` не стреляет)
  overlay перепозиционируется по последней позиции курсора (`lastCursorX`) в
  обработчике `onRangeChange`. Предохранители против петель: `loadingRef`,
  `oldestAvailableDate`, `from >= loadedFrom`. **Lazy-подгрузка** (только при
  `lazyLoad=true`) ведёт leading-edge `chartZoom.throttle` (220 мс) — запрос
  уходит на первом импульсе, а не ждёт тишины (как debounce); при сильном
  отдалении чанки подгружаются циклом автодогрузки в `requestLazyLoad` (до
  `MAX_AUTOPRELOAD_CHAIN=4` за заход, проверка `needsLazyLoad` после каждого
  merge), `PRELOAD_FRACTION=0.75`. Контейнер графика всегда в DOM
  (состояния loading/error/empty — overlay поверх canvas), чтобы `createChart` не
  попадал в цикл ожидания.
- `ui/StockDetailsView.tsx` — контент страницы: Header (**логотип компании**
  через `<Avatar variant="rounded" src={resolveLogoUrl(stock.logoUrl)} />`
  с `onError`-фallback на `/logos/default.svg`; компания, тикер-Chip,
  биржа-Chip, **сектор-Chip** с иконкой `Building2`), Price Block (текущая
  цена крупным шрифтом через `formatMoney`), User Position (через
  `usePortfolioQuery` с локальной фильтрацией по `stockId`; количество,
  средняя цена, текущая стоимость, PnL через `formatProfitLoss`; если позиции
  нет — «You do not own this stock.»), Trading (кнопки Buy/Sell, открывающие
  **переиспользованные** `BuyStockDialog`/`SellStockDialog` из
  `features/trading/ui` — без дублирования логики торговли), **About**
  (`stock.description`, `whiteSpace: pre-line`, рендерится только при
  наличии), Company Information (ticker, companyName, exchange, lotSize,
  **sector**, **website**-ссылкой `target="_blank" rel="noopener noreferrer"`
  с иконкой `Globe` — только поля backend). `resolveLogoUrl()` склеивает
  `VITE_API_URL` (базовый URL backend) с API-relative путём логотипа
  (`/logos/SBER.svg`) — это нужно, т.к. в dev фронт и backend на разных
  портах и `<img src>` не проходит через axios-клиент с его интерсепторами.
  В `PositionDetails` рядом с Quantity показывается «(N lots)»
  (`quantity / lotSize`, когда делится без остатка).
- `types/` — зарезервировано (`.gitkeep`); контракт акции `Stock`
  переиспользуется из `features/stocks/types/stockTypes.ts`.

Страница `pages/StockDetailPage.tsx` — тонкая обёртка: читает тикер через
`useParams<{ ticker: string }>()`, дёргает `useStockDetailsQuery`, рендерит
состояния (loading → `TableSkeleton`, сетевая ошибка → `StateError` с Retry,
акция не найдена → `StateError` «Stock not found.» с кнопкой Back to Stocks,
успех → `StockDetailsView`). Кнопка Back to Stocks (`Link` на `/stocks`) в
«шапке» страницы видна во всех состояниях. `useParams` — первое использование
в проекте.

Принципы фичи:

- **Контракт `StockResponse` расширен**: backend (V7) добавил в payload акции
  `description`, `sector`, `website` и вычисляемый `logoUrl` (путь к SVG в
  `static/logos/`, который backend раздаёт публично по `/logos/{ticker}.svg`).
  Эти поля прокинуты в тип `Stock` (`features/stocks/types/stockTypes.ts`),
  поэтому распространяются на каталог, портфель и detail без новых запросов и
  без обращения к внешним API — backend единый источник информации о компании.
- **Статические ресурсы через `VITE_API_URL`**: логотип — это API-relative
  путь (`/logos/SBER.svg`), который браузер грузит обычным `<img>` (не через
  axios, поэтому без JWT/X-Guest-Token). Абсолютный URL собирается как
  `${VITE_API_URL}${logoUrl}`. При ошибке загрузки (404) срабатывает
  `onError` → fallback на `/logos/default.svg`.
- **Reuse всего**: тип `Stock`, диалоги `BuyStockDialog`/`SellStockDialog`,
  `formatMoney`/`formatProfitLoss`/`formatDateTime`, `usePortfolioQuery`,
  компоненты `TableSkeleton`/`StateError` из `@/shared/components`. Адаптер
  `toStock` (как в `PortfolioTable`) не нужен — объект уже формы `Stock`.
- **Empty State не используется** — для отсутствующей акции показывается Error
  State («Stock not found.»).
- **Навигация**: тикер в `StocksTable` обёрнут в `<Link to={`/stocks/${ticker}`}>`
  — клик по нему открывает детальную страницу. Кнопки Buy/Sell в той же строке
  продолжают работать без перехода (отдельный `onClick`, всплытие к Link не
  идёт). Кликабельный тикер выбран вместо кликабельной строки, чтобы не плодить
  `stopPropagation` в кнопках действий.
- **Структура без `hooks/`**: фича следует устоявшейся конвенции проекта —
  React Query-хуки живут в `model/`, папки `hooks/` нет ни в одной из 6
  существующих фич (несмотря на упоминание `hooks/` в ранних набросках этой
  задачи). Деление `api/model/types/ui` консистентно со всем проектом.
- **График цены**: viewport (активный период, `loadedFrom`/`loadedTo`,
  `oldestAvailableDate`) и кэш склеенных данных живут в `useRef`, а не в
  React-state — движение мыши не должно перерисовывать компонент; React-state
  включается только для переключения UI вокруг графика (кнопки периода,
  Skeleton/Error/Empty/индикатор фоновой подгрузки). Модель «кнопка = интервал
  свечи»: серия всегда одного bucket size, без артефактов. **Колесо мыши = зум**
  относительно точки под курсором (встроенный `handleScale.mouseWheel`); догружает
  прошлое **ЛКМ-драг** за левый край (и фоновая предзагрузка). Crosshair-интерфейс
  (вертикальная пунктирная направляющая + круглый маркер + tooltip) — собственный
  DOM-overlay, позиционируется из одного обработчика `mousemove` без re-render;
  при зуме перепозиционируется по последней позиции курсора в `onRangeChange`.
  Защита от бесконечных lazy-запросов — несколько предохранителей разом (флаг
  загрузки, `oldestAvailableDate`, `needsLazyLoad`, `from >= loadedFrom`) +
  ведущий `throttle` (220 мс) и цикл автодогрузки при сильном отдалении.
- **Подготовка к следующему этапу**: фича изолирует страницу акции и уже
  содержит график цены (`StockPriceChart`); будущие индикаторы/объёмы
  добавятся в тот же UI-блок новыми сериями lightweight-charts.

## Где хранится API

- `shared/api/` — общий `apiClient` (Axios), интерсепторы, `endpoints`, общие типы.
- `features/<feature>/api/` — функции-обёртки над `apiClient` для конкретной фичи.

## Где хранится общая логика

- `shared/lib/` — `queryKeys`, `apiError`, `storage`, и `chart/` (общие
  графиковые хелперы: `chartTheme.ts`, `chartZoom.ts`, `periods.ts`
  (`HistoryPeriodCode`), `historyPoint.ts` (`HistoryPoint`) — переиспользуются
  графиком цены акции и графиком стоимости портфеля через `PriceLineChart`).
- `shared/utils/` — чистые функции: `formatMoney` (денежный формат + символ `ᛔ`),
  `formatProfitLoss` (PnL со знаком `+`/`−` и цветом по теме MUI — используется
  в `account`, `portfolio` и `stock-details`), `formatPercent` (относительное
  изменение цены со знаком `+`/`−`, `%` и цветом по теме MUI — используется в
  `DashboardPortfolioPanel`), `formatDateTime` (дата/время `yyyy-mm-dd hh:mm` —
  используется в `transactions` и в tooltip'ах графиков). Это **единственное**
  место форматирования денег/дат/PnL/процентов — локальные копии в фичах запрещены.
- `shared/components/` — переиспользуемые UI-компоненты состояний: `StateError`,
  `StateEmpty`, `TableSkeleton` (см. раздел *shared/components* выше), и
  `charts/PriceLineChart.tsx` — обобщённый графиковый примитив (Lightweight
  Charts), общая база для графика цены акции (`StockPriceChart`) и графика
  стоимости портфеля (`PortfolioValueChart`).
- `shared/hooks/` — (зарезервировано) общие хуки.

## Где хранить типы

- `shared/types/` — глобальные типы, используемые в нескольких фичах.
- `features/<feature>/types/` — типы, принадлежащие только одной фиче.
- **Запрещено** импортировать типы из фичи A в фичу B. Если тип нужен в нескольких
  фичах — он поднимается в `shared/types/`.

## Конвенции кода

- **MUI**: `sx`-проп вместо `styled()`; кнопки используют `loading`-проп для
  индикатора; Grid в MUI 7 — через `size={{ xs: 12, sm: 6 }}` (не legacy `xs={12}`).
- **Иконки**: только `lucide-react` (`<RefreshCw size={16} />`).
- **Деньги**: всегда через `formatMoney(value)` из `shared/utils/format.ts`
  (группировка пробелом, точка как разделитель; в конце — символ вымышленной валюты
  «Дубли» `ᛔ`, константа `CURRENCY_SYMBOL`). Backend отдаёт `BigDecimal`, который
  Jackson сериализует в JSON-число → фронтовые типы используют `number`.
- **Дата/время**: всегда через `formatDateTime(value)` из `shared/utils/format.ts`
  (детерминированный `yyyy-mm-dd hh:mm`, единый вид во всех компонентах; не
  использовать браузерную локаль `toLocaleString()` напрямую в фичах).
- **Прибыль/убыток (PnL)**: отображается через общую `formatProfitLoss(value)` из
  `shared/utils/format.ts` — возвращает текст со знаком (`+`/`−`/без знака) и цвет
  по теме MUI (`success.main` / `error.main` / `text.primary`). Жёстко цвета не
  прописываются. Не использовать локальные копии хелпера в фичах.
- **Относительное изменение цены (%)**: отображается через `formatPercent(value)`
  из того же `format.ts` — знак `+`/`−`, один знак после точки, `%`, цвет по теме
  MUI. Используется в `DashboardPortfolioPanel` (изменение цены позиции к средней).
- **Формы**: React Hook Form + Zod (`zodResolver` из `@hookform/resolvers/zod`),
  zod-схема — в `features/<feature>/lib/`. Валидируется только ввод; бизнес-проверки
  (баланс, наличие позиции) — на backend, не дублируются.
- **Именование файлов**: компоненты — `PascalCase.tsx`; хуки — `useXxx.ts`;
  утилиты/типы/схемы — `camelCase.ts` / `xxxTypes.ts` / `xxxSchema.ts`.
- **Состояния loading/error/empty**: только через компоненты из
  `@/shared/components` (`TableSkeleton`, `StateError`, `StateEmpty`). Inline-копии
  блоков `<Skeleton>`/`AlertCircle`+`Retry`/empty-текста **не дублировать**. Цвета
  состояний — из темы MUI (`error.main`, `text.secondary`), **без хардкод-hex**
  (например `#d32f2f`).
- **React Query опции**: `staleTime`, `refetchOnWindowFocus`, `retry` заданы как
  дефолты в `createQueryClient()` (`app/providers/queryProvider.tsx`) — **не
  дублировать** их в каждом `useQuery`. Хук содержит только `queryKey` + `queryFn`
  (особые случаи оговариваются отдельно).
- **React Query инвалидация после trade**: buy/sell-мутации инвалидируют
  `account`/`stocks`/`portfolio`/`transactions` (затронутые сущности
  пользователя). Каталог `stocks` инвалидируется prefix-ключом, который
  захватывает и список акций (`['stocks', {page,size}]`), и detail-запрос по
  тикеру (`['stocks','detail',ticker]`) — это нужно для автообновления страницы
  Stock Detail после сделки; сами рыночные цены от сделки не меняются (их
  обновляет планировщик backend).
- **Пагинация stocks (особый случай)**: `useStocksQuery(page, size)` передаёт
  параметры в ключ (`[...queryKeys.stocks, { page, size }]`) и использует
  `placeholderData: keepPreviousData` — при смене страницы предыдущая держится на
  экране, пока грузится новая (без мигания скелетона), а `isPlaceholderData`
  блокирует контролы пагинации. Backend отдаёт Spring Page (`StockPage`:
  `{ content, totalElements, totalPages, size, number }`); UI — MUI
  `TablePagination` внутри `StocksTable` (rows-per-page: 10/20/50, дефолт 20).
  `page`/`rowsPerPage` хранятся в `StocksPage`, источник истины для текущей
  страницы — `data.number` из ответа backend.

## Реализованные фичи и статусы

| Фича | Статус |
|---|---|
| `auth` | ✓ JWT + Guest, bootstrap через `/users/me`, login, register (с конвертацией гостя), logout с reload на `/dashboard` |
| `account` | ✓ `GET /account`, `AccountSummary` на Dashboard + сводка активов в топбаре |
| `stocks` | ✓ `GET /stocks`, каталог + цена + Lot Size + **sparkline** (SVG) + Buy/Sell + постраничная навигация |
| `stock-details` | ✓ `/stocks/:ticker` — Header (логотип + сектор), Price Block, User Position (с «N lots»), Trading (reuse диалогов), **Price Chart** (линейный график close, тонкая обёртка над общим `shared/components/charts/PriceLineChart.tsx`; lightweight-charts, цвет `#3dba8d`; кнопки 1D/1W/1M/1Y = интервал свечи день/неделя/месяц/квартал; **колесо = зум относительно курсора**, ЛКМ-драг — бесконечный lazy-скролл в прошлое тем же интервалом; crosshair-overlay: чёрная пунктирная направляющая + маркер + tooltip), About (описание), Company Information (Ticker, Name, Exchange, Lot Size, Sector, Website) |
| `trading` | ✓ buy/sell **в лотах** через mutation + диалоги (поле Lots, подсказка `1 lot = N shares`, live-расчёт shares и total) |
| `portfolio` | ✓ `GET /portfolio` (таблица позиций: Quantity с «N lots» + Sell) + `GET /portfolio/history` (**график стоимости портфеля** над таблицей — тот же `PriceLineChart` с `lazyLoad=false`, периоды 1D/1W/1M/1Y, tooltip «Стоимость»); пустой портфель → Empty State с кнопкой «Перейти к акциям». `DashboardPortfolioPanel` — read-only обзор на Dashboard |
| `transactions` | ✓ `GET /transactions`, таблица истории операций |
| `guest` | (пусто) — создание гостя в `shared/api/apiClient.ts` → `createGuest()` |

Отдельная страница `Account` убрана (сводка по аккаунту перенесена на Dashboard
через `AccountSummary` + `DashboardPortfolioPanel`, а краткая стоимость активов
постоянно видна в топбаре). Рабочие маршруты: `/dashboard`, `/stocks`,
`/stocks/:ticker`, `/portfolio`, `/transactions`, `/login`, `/register`.

## Запрещено использовать

- Redux
- MobX
- Zustand
- Bootstrap
- Любые другие глобальные state-менеджеры вне React Context + TanStack Query

## Правило: пишем код небольшими feature

- Каждая фича — это закрытый модуль.
- Фича не должна импортировать внутренние файлы другой фичи.
- Общение между фичами происходит только через `shared/` (типы, утилиты, `queryKeys`).
- При росте фичи дробим её на подфичи или выносим код в `widgets/` / `shared/`.

---

*Для контрактов backend и бизнес-логики смотри корневой `readme.md` и `claude.md`.*
