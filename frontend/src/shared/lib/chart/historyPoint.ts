/**
 * Точка на линейном графике (Area, только close/value): ISO-время + значение.
 *
 * Общий тип для графика цены акции (`features/stock-details`) и графика
 * стоимости портфеля (`features/portfolio`) — оба скармливают `PriceLineChart`
 * один и тот же формат. `time` остаётся ISO-строкой — она однозначно
 * маппится на lightweight-charts `UTCTimestamp`.
 */
export interface HistoryPoint {
  /** ISO-строка UTC-Instant, напр. `2025-01-01T10:00:00Z`. */
  time: string;
  /** Значение (цена закрытия свечи или стоимость портфеля). */
  value: number;
}
