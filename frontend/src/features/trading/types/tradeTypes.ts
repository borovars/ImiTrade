/**
 * Контракт торговых операций (POST /api/v1/trades/buy, /sell).
 *
 * Соответствует backend-records BuyStockRequest / SellStockRequest / TradeResponse.
 * Денежные значения backend отдаёт как BigDecimal, который Jackson сериализует
 * в JSON-число, поэтому price/totalAmount типизируются как number.
 *
 * Торговля идёт в лотах: frontend отправляет `lots`, backend вычисляет
 * `quantity = lots × lotSize` и возвращает фактическое число акций.
 */
export interface BuyStockRequest {
  stockId: number;
  lots: number;
}

export interface SellStockRequest {
  stockId: number;
  lots: number;
}

export interface TradeResponse {
  transactionId: number;
  stockTicker: string;
  /** "BUY" | "SELL" — TransactionType.name() на backend. */
  type: string;
  /** Фактическое число акций (lots × lotSize) — источник истины. */
  quantity: number;
  price: number;
  totalAmount: number;
  /** Shares per lot (эхо из stock.lotSize для удобства отображения). */
  lotSize: number;
  /** Число лотов в сделке = quantity / lotSize. */
  lots: number;
}
