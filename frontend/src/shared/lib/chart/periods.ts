/**
 * Короткий код периода графика, как его принимает backend
 * (`HistoryPeriod.parse` в `StockHistoryController` / `PortfolioController`).
 *
 * Живёт в `shared`, т.к. нужен и графиковым примитивам (`PriceLineChart`), и фиче
 * stock-details, и фиче portfolio — а фичи не должны импортировать внутренности
 * друг друга (правило feature-isolation из `frontend/CLAUDE.md`).
 *
 * Модель «кнопка = интервал свечи» (стиль Т-Инвестиций / MOEX):
 * - `1D` — дневные свечи, последние 3 месяца;
 * - `1W` — недельные свечи, последние 5 месяцев;
 * - `1M` — месячные свечи, последние 3 года;
 * - `1Y` — квартальные свечи, последние 10 лет.
 *
 * Должно совпадать с backend `HistoryPeriod` (см. `HistoryPeriod.java`).
 */
export type HistoryPeriodCode = '1D' | '1W' | '1M' | '1Y';
