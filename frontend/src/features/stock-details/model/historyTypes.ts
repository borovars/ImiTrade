/**
 * Типы для истории цены акции, отдаваемой backend-эндпоинтом
 * `GET /api/v1/stocks/{ticker}/history`.
 *
 * Backend возвращает полную OHLCV-свечу (`CandleResponse`:
 * `time, open, high, low, close, volume`), но линейному графику цены нужны
 * только `time` и `close` — поэтому API-слой на фронте отсекает лишние поля
 * и приводит ответ к {@link HistoryPoint}.
 */

/**
 * Короткий код периода, как его принимает backend
 * (`HistoryPeriod.parse` в `StockHistoryController`).
 *
 * Модель «кнопка = интервал свечи» (стиль Т-Инвестиций / MOEX):
 * - `1D` — дневные свечи, последние 3 месяца;
 * - `1W` — недельные свечи, последние 5 месяцев;
 * - `1M` — месячные свечи, последние 3 года;
 * - `1Y` — квартальные свечи, последние 10 лет.
 *
 * `3M` удалён — его диапазон покрывается 1W/1M. Должно совпадать с backend
 * `HistoryPeriod` (см. `HistoryPeriod.java`).
 */
export type HistoryPeriodCode = '1D' | '1W' | '1M' | '1Y';

/**
 * Сырая свеча из ответа backend (полный `CandleResponse`).
 *
 * `time` — ISO-строка UTC-Instant (Jackson сериализует `Instant` как
 * `2025-01-01T00:00:00Z`). Числовые поля приходят строками/числами —
 * нормализуются в API-слое.
 */
export interface HistoryCandleDto {
  time: string;
  open: number | string;
  high: number | string;
  low: number | string;
  close: number | string;
  volume: number | string | null;
}

/**
 * Точка на линейном графике: только то, что нужно для отрисовки close.
 * `time` остаётся ISO-строкой — она однозначно маппится на lightweight-charts.
 */
export interface HistoryPoint {
  /** ISO-строка UTC-Instant, напр. `2025-01-01T10:00:00Z`. */
  time: string;
  /** Цена закрытия свечи. */
  close: number;
}
