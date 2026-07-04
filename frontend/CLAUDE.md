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
├── widgets/                # Самостоятельные блоки UI (layout, sidebar, topbar)
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
т.к. структура карточек отличается от таблицы).

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
  `/dashboard`, `/stocks`, `/portfolio`, `/transactions`, `/account`

`AppLayout` (`widgets/layout/`) — постоянный `Drawer` (сайдбар) + фиксированный
`AppBar` (топбар), `drawerWidth = 240`.

## Аутентификация (Auth)

Реализована в `features/auth/model/` поверх `useReducer` + React Context:

- **`authStore.tsx`** — `authReducer`, `AuthContext`, хуки `useAuth()`,
  `useAuthState()`, `useAuthDispatch()`. Действия: `SET_GUEST | SET_AUTH | RESET | SET_READY`.
- **`authService.ts`** — `bootstrapAuth`, `initGuestIfNeeded`, `saveJwtToken`,
  `saveGuestToken`, `resetAuth`. При старте приложения: если есть JWT — `SET_AUTH`;
  иначе если есть guest-token — `SET_GUEST`; иначе автоматически создаётся гость.
- **`types.ts`** — `UserType = 'guest' | 'auth'`, `User`, `AuthState`.

Токены хранятся в `localStorage` через `shared/lib/storage.ts` (ключи
`imitrade_jwt_token`, `imitrade_guest_token`). JWT имеет приоритет над guest-token.

## shared/api — HTTP-слой

- **`apiClient.ts`** — экземпляр Axios (`baseURL = VITE_API_URL`, `Content-Type: application/json`).
  - **Request-интерсептор** автоматически подставляет `Authorization: Bearer <jwt>`
    (приоритет) или `X-Guest-Token <guest>`.
  - **Response-интерсептор** нормализует ошибку через `normalizeApiError` в `ApiError`
    (`message`, `status`) и избирательно тостит: 401 — чистит auth и редиректит на `/`
    (только если ранее был токен, и не для запроса создания гостя); 403 — тост
    «Доступ запрещён»; ≥500 — тост «Ошибка сервера». **4xx (400/404/409) НЕ
    тостятся автоматически** — их показывает вызывающий код (например, `onError` мутации).
  - Также экспортирует хелпер `createGuest()` (POST `/api/v1/guest`) и реэкспорт `ApiError`.
- **`endpoints.ts`** — `API_ENDPOINTS` (все пути `/api/v1/...` в одном месте, включая
  `TRADING.BUY`/`TRADING.SELL`). Новые пути добавлять сюда, **не** дублировать строками.
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
    portfolio: ['portfolio'] as const,
    transactions: ['transactions'] as const,
  };
  ```

  Это **канал взаимодействия между фичами**: мутация фичи trading инвалидирует
  чужие ключи через `queryKeys.account` и т.д., а не через внутренние константы
  других фич. **Строковые литералы ключей не используем** — только `queryKeys.*`.
- **Mutation + инвалидация** (образец — `features/trading/model/useBuyStockMutation.ts`):
  в `onSuccess` вызываем `queryClient.invalidateQueries({ queryKey: queryKeys.account })`
  для затронутых сущностей и `toast.success(...)`; в `onError` — `toast.error(error.message)`.
  Ручное обновление состояния (`setQueryData`) **не используем** (optimistic updates запрещены).

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
  (по контракту backend: `{ stockId, quantity }`; `TradeResponse` —
  `{ transactionId, stockTicker, type, quantity, price, totalAmount }`).
- `api/tradingApi.ts` — `buyStock()`, `sellStock()` через `apiClient.post`.
- `lib/tradeSchema.ts` — Zod-валидация количества (`coerce.number().int().positive()`).
- `model/useBuyStockMutation.ts`, `model/useSellStockMutation.ts` — `useMutation` с
  инвалидацией `account`/`stocks`/`portfolio`/`transactions` и тостами.
- `ui/TradeStockDialog.tsx` — общая форма сделки (MUI `Dialog` + RHF + `zodResolver`),
  переиспользуется покупкой и продажей. `ui/BuyStockDialog.tsx`, `ui/SellStockDialog.tsx`
  — тонкие обёртки, **без дублирования** логики.

Кнопки Buy/Sell выводятся в таблице акций (`features/stocks/ui/StocksTable.tsx`);
диалоги монтируются там же с локальным состоянием. После успешной сделки диалог
закрывается, тост успеха показывается в `onSuccess` хука.

## Feature: portfolio (только чтение)

Read-only фича позиций пользователя. Структура:

- `types/portfolioTypes.ts` — `PortfolioPosition` (по контракту backend:
  `{ stockId, ticker, companyName, quantity, averagePrice, currentPrice, pnl }`).
  `GET /api/v1/portfolio` отдаёт **простой массив** (не Spring Page).
- `api/portfolioApi.ts` — `getPortfolio()` через `apiClient.get`.
- `model/usePortfolioQuery.ts` — `useQuery` с `queryKeys.portfolio`.
- `ui/PortfolioTable.tsx` — Material UI Table со столбцами Ticker, Company,
  Quantity, Average Price, Current Price, Position Value, Profit/Loss, Actions.

Принципы фичи:

- **Финансовые показатели берутся из backend**: `pnl` уже рассчитан
  (`(currentPrice - averagePrice) * quantity`) и не пересчитывается на фронте.
  Единственное производное значение — Position Value = `quantity * currentPrice`
  (display-умножение из примитивов backend, не пересчёт агрегатов).
- **Profit/Loss** — цветовая индикация через тему MUI (зелёный/красный/нейтрально)
  общей функцией `formatProfitLoss` из `shared/utils/format.ts`. Цвета берутся из
  темы (`success.main` / `error.main` / `text.primary`), жёстко не прописываются.
- **Продажа** — кнопка Sell в каждой строке открывает переиспользованный
  `SellStockDialog` из `features/trading/ui/`. Новая логика продажи не пишется.
  Позиция портфеля адаптируется в объект формы `Stock` (`stockId` → `id`,
  `exchange` пустой — он не нужен для продажи).
- **Автообновление** — после успешной продажи существующие trading-мутации уже
  инвалидируют `queryKeys.portfolio`/`account`/`stocks`/`transactions`, поэтому
  таблица портфеля и Dashboard обновляются без правок в trading.
- **Состояния** (loading/error/empty) обрабатываются на странице
  `pages/PortfolioPage.tsx`: Skeleton при загрузке, блок ошибки с кнопкой Retry,
  empty-state с приглашением купить первую акцию на странице Stocks.

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

## Где хранится API

- `shared/api/` — общий `apiClient` (Axios), интерсепторы, `endpoints`, общие типы.
- `features/<feature>/api/` — функции-обёртки над `apiClient` для конкретной фичи.

## Где хранится общая логика

- `shared/lib/` — `queryKeys`, `apiError`, `storage`.
- `shared/utils/` — чистые функции: `formatMoney` (денежный формат),
  `formatProfitLoss` (PnL со знаком `+`/`−` и цветом по теме MUI — используется
  в `account` и `portfolio`), `formatDateTime` (дата/время `yyyy-mm-dd hh:mm` —
  используется в `transactions`). Это **единственное** место форматирования
  денег/дат/PnL — локальные копии в фичах запрещены.
- `shared/components/` — переиспользуемые UI-компоненты состояний: `StateError`,
  `StateEmpty`, `TableSkeleton` (см. раздел *shared/components* выше).
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
  (группировка пробелом, точка как разделитель; без символа валюты). Backend отдаёт
  `BigDecimal`, который Jackson сериализует в JSON-число → фронтовые типы используют `number`.
- **Дата/время**: всегда через `formatDateTime(value)` из `shared/utils/format.ts`
  (детерминированный `yyyy-mm-dd hh:mm`, единый вид во всех компонентах; не
  использовать браузерную локаль `toLocaleString()` напрямую в фичах).
- **Прибыль/убыток (PnL)**: отображается через общую `formatProfitLoss(value)` из
  `shared/utils/format.ts` — возвращает текст со знаком (`+`/`−`/без знака) и цвет
  по теме MUI (`success.main` / `error.main` / `text.primary`). Жёстко цвета не
  прописываются. Не использовать локальные копии хелпера в фичах.
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
  `account`/`portfolio`/`transactions` (затронутые сущности пользователя). Каталог
  `stocks` **не инвалидируется** — это рыночные данные, не зависящие от сделок
  пользователя (цены обновляются планировщиком backend).
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
| `auth` | ✓ JWT + Guest, bootstrap, logout |
| `account` | ✓ `GET /account`, AccountSummary на Dashboard |
| `stocks` | ✓ `GET /stocks`, каталог + цена + Buy/Sell + постраничная навигация |
| `trading` | ✓ buy/sell через mutation + диалоги |
| `portfolio` | ✓ `GET /portfolio`, таблица позиций + Sell |
| `transactions` | ✓ `GET /transactions`, таблица истории операций |
| `guest` | (пусто) — создание гостя в `shared/api/apiClient.ts` → `createGuest()` |

Страницы `Login`, `Register`, `Account` — заглушки с заголовком. Рабочие
страницы: `Dashboard`, `Stocks`, `Portfolio`, `Transactions`.

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
