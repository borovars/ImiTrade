/**
 * Контракт торговых операций (POST /api/v1/trades/buy, /sell).
 *
 * Соответствует backend-records BuyStockRequest / SellStockRequest / TradeResponse.
 * Денежные значения backend отдаёт как BigDecimal, который Jackson сериализует
 * в JSON-число, поэтому price/totalAmount типизируются как number.
 */
export interface BuyStockRequest {
  stockId: number;
  quantity: number;
}

export interface SellStockRequest {
  stockId: number;
  quantity: number;
}

export interface TradeResponse {
  transactionId: number;
  stockTicker: string;
  /** "BUY" | "SELL" — TransactionType.name() на backend. */
  type: string;
  quantity: number;
  price: number;
  totalAmount: number;
}
