## План: панель портфеля на Dashboard

### Цель
Добавить снизу от виджетов `AccountSummary` на `/dashboard` read-only панель с содержимым портфеля: тикер, компания, количество, средняя цена покупки, текущая цена, **изменение цены (абсолют + процент)** и **прибыль/убыток** по каждой акции.

### Изменения

**1. `frontend/src/shared/utils/format.ts`** — добавить хелпер `formatPercent(value)`

Точно по образцу `formatProfitLoss` возвращает `{ text, color }`:
- `> 0` → `+ 5.5%`, `success.main`
- `< 0` → `− 2.9%`, `error.main`
- `0` → `0.0%`, `text.primary`

Используется для колонки «Изменение цены» (цвет по знаку). Формат 1 знак после точки.

**2. Новый компонент `frontend/src/features/portfolio/ui/DashboardPortfolioPanel.tsx`** (read-only)

Зеркалит структуру `AccountSummary.tsx` и паттерн состояний `PortfolioPage.tsx`:
- Props: `{ data?, isLoading, isError, error?, refetch }`
- `isLoading` → `<TableSkeleton />`
- `isError` → `<StateError title="Не удалось загрузить портфель" error={error} onRetry={refetch} />`
- `data && data.length === 0` → `<StateEmpty title="Ваш портфель пуст." helperText="Купите первую акцию на странице «Акции»." />`
- `data && data.length > 0` → заголовок секции (`Typography variant="h6"` «Содержимое портфеля» в `Box sx={{ mb: 2 }}`) + `TableContainer component={Paper}` с таблицей

Колонки таблицы (read-only, **без** кнопки «Продать», `toStock` и `SellStockDialog`):
| Тикер | Компания | Количество | Ср. цена | Текущая цена | Изменение цены | Прибыль / Убыток |

По строке (`position: PortfolioPosition`):
- `Тикер` — `position.ticker`, `fontWeight: 600`
- `Компания` — `position.companyName`
- `Количество` — `position.quantity` + подпись `(N лот.)` при `lotSize > 0 && quantity % lotSize === 0` (как в `PortfolioTable`)
- `Ср. цена` — `formatMoney(position.averagePrice)`
- `Текущая цена` — `formatMoney(position.currentPrice)`
- `Изменение цены` — `priceChange = position.currentPrice − position.averagePrice`; абс. значение через существующий паттерн знака (`formatMoney(Math.abs(priceChange))` с `+`/`−`), рядом в скобках процент `formatPercent(priceChange / averagePrice * 100).text`; цвет по `formatPercent(...).color`
- `Прибыль / Убыток` — `formatProfitLoss(position.pnl)` → `{ text, color }` с `fontWeight: 600` (как в `PortfolioTable`)

Импорты: `usePortfolioQuery` НЕ подключается здесь — данные прокидываются из `DashboardPage` (как `AccountSummary`), компонент остаётся чистым. Переиспользуются `formatMoney`, `formatProfitLoss`, новый `formatPercent`, `TableSkeleton`, `StateError`, `StateEmpty`.

**3. `frontend/src/pages/DashboardPage.tsx`** — подключить панель

- Добавить `const { data: portfolioData, isLoading, isError, error, refetch } = usePortfolioQuery();` (импорт из `@/features/portfolio/model/usePortfolioQuery`)
- Под существующим `<AccountSummary .../>` добавить:
  ```tsx
  <Box sx={{ mt: 4 }}>
    <DashboardPortfolioPanel
      data={portfolioData}
      isLoading={isLoading}
      isError={isError}
      error={error}
      refetch={refetch}
    />
  </Box>
  ```

### Чего НЕ делаем
- Не трогаем backend — `GET /api/v1/portfolio` уже возвращает `currentPrice`, `averagePrice`, `pnl` (всё рассчитано на backend)
- Не меняем `PortfolioTable` — он остаётся для страницы «Портфель» с кнопкой «Продать»
- Не меняем тему — `success.main`/`error.main` уже дают зелёный/красный через дефолтную light-тему MUI
- Не добавляем новые query-ключи/API — `queryKeys.portfolio` уже инвалидируется trading-мутациями

### Проверка
- `cd frontend && npm run lint` — без ошибок линтера
- `npm run build` — `tsc + vite build` проходит (строгий TS)
- Визуально: на `/dashboard` снизу появляется таблица позиций; после buy/sell обновляется автоматически (через инвалидицию `queryKeys.portfolio`)