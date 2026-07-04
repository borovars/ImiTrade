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
    ├── components/         # (зарезервировано) переиспользуемые UI-компоненты
    ├── hooks/              # (зарезервировано) общие хуки
    └── types/              # (зарезервировано) глобальные типы
```

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

## Где хранится API

- `shared/api/` — общий `apiClient` (Axios), интерсепторы, `endpoints`, общие типы.
- `features/<feature>/api/` — функции-обёртки над `apiClient` для конкретной фичи.

## Где хранится общая логика

- `shared/lib/` — `queryKeys`, `apiError`, `storage`.
- `shared/utils/` — чистые функции (например, `formatMoney`).
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
- **Формы**: React Hook Form + Zod (`zodResolver` из `@hookform/resolvers/zod`),
  zod-схема — в `features/<feature>/lib/`. Валидируется только ввод; бизнес-проверки
  (баланс, наличие позиции) — на backend, не дублируются.
- **Именование файлов**: компоненты — `PascalCase.tsx`; хуки — `useXxx.ts`;
  утилиты/типы/схемы — `camelCase.ts` / `xxxTypes.ts` / `xxxSchema.ts`.

## Реализованные фичи и статусы

| Фича | Статус |
|---|---|
| `auth` | ✓ JWT + Guest, bootstrap, logout |
| `account` | ✓ `GET /account`, AccountSummary на Dashboard |
| `stocks` | ✓ `GET /stocks`, каталог + цена + Buy/Sell |
| `trading` | ✓ buy/sell через mutation + диалоги |
| `portfolio` | заглушка (страница без данных) |
| `transactions` | заглушка (страница без данных) |
| `guest` | (пусто) — создание гостя в `shared/api/apiClient.ts` → `createGuest()` |

Страницы `Login`, `Register`, `Account`, `Portfolio`, `Transactions` — заглушки с
заголовком. Рабочие страницы: `Dashboard`, `Stocks`.

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
