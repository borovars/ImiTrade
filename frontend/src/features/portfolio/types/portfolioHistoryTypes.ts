import type { HistoryPoint } from '@/shared/lib/chart/historyPoint';

/**
 * Точка истории стоимости портфеля, отдаваемая backend-эндпоинтом
 * `GET /api/v1/portfolio/history` (`PortfolioHistoryResponse`):
 * `{ time: ISO-Instant, value: Σ qty_held × close }`.
 *
 * Формат совпадает с {@link HistoryPoint} — поэтому реэкспортируем его и не
 * заводим отдельный интерфейс: график (`PriceLineChart`) работает с одним типом.
 */
export type PortfolioHistoryPoint = HistoryPoint;

/** Сырой DTO из ответа backend (до нормализации чисел). */
export interface PortfolioHistoryPointDto {
  time: string;
  value: number | string;
}
