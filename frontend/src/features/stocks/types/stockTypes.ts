export interface Stock {
  id: number;
  ticker: string;
  companyName: string;
  exchange: string;
  currentPrice: number;
  /** Shares per lot — lotSize × lots = share quantity. Source of truth for trading. */
  lotSize: number;
}

/**
 * Обёртка Spring Page, которую возвращает GET /api/v1/stocks.
 *
 * Backend отдаёт Page (по умолчанию size=20). Метаданные пагинации
 * используются каталогом акций для постраничной навигации.
 */
export interface StockPage {
  content: Stock[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
