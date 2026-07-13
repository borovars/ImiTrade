/**
 * Контракт позиции портфеля (GET /api/v1/portfolio).
 *
 * Соответствует backend-record PortfolioResponse. Эндпоинт отдаёт простой
 * массив позиций (не Spring Page). Денежные значения backend сериализует из
 * BigDecimal в JSON-число, поэтому averagePrice/currentPrice/pnl — number.
 *
 * pnl уже рассчитан на backend: `(currentPrice - averagePrice) * quantity`.
 * Frontend не пересчитывает финансовый результат — только отображает.
 */
export interface PortfolioPosition {
  stockId: number;
  ticker: string;
  companyName: string;
  quantity: number;
  averagePrice: number;
  currentPrice: number;
  pnl: number;
  /** Shares per lot — отображение «N lots» = quantity / lotSize. */
  lotSize: number;
}
