/**
 * Типы для истории цены акции, отдаваемой backend-эндпоинтом
 * `GET /api/v1/stocks/{ticker}/history`.
 *
 * Backend возвращает полную OHLCV-свечу (`CandleResponse`:
 * `time, open, high, low, close, volume`), но линейному графику цены нужны
 * только `time` и `close` — поэтому API-слой на фронте отсекает лишние поля
 * и приводит ответ к {@link HistoryPoint}.
 *
 * Общие типы для графиков (`HistoryPeriodCode`, `HistoryPoint`) живут в
 * `shared/lib/chart` и реэкспортируются здесь для обратной совместимости с
 * внутренними импортами фичи stock-details. Семантика периода
 * (1D/1W/1M/1Y → интервал свечи + lookback) описана в
 * `shared/lib/chart/periods.ts`.
 */
export type { HistoryPeriodCode } from '@/shared/lib/chart/periods';
export type { HistoryPoint } from '@/shared/lib/chart/historyPoint';

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
