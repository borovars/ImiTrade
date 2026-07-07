export interface Stock {
  id: number;
  ticker: string;
  companyName: string;
  exchange: string;
  currentPrice: number;
  /** Shares per lot — lotSize × lots = share quantity. Source of truth for trading. */
  lotSize: number;
  /** Краткое описание компании (backend V7). */
  description?: string;
  /** Сектор экономики компании (backend V7). */
  sector?: string;
  /** Официальный сайт компании (backend V7). */
  website?: string;
  /**
   * URL логотипа компании — вычисляется на backend по тикеру
   * (путь к SVG в static/logos/, например `/logos/SBER.svg`),
   * fallback — `/logos/default.svg`. API-relative: префикс `VITE_API_URL`.
   */
  logoUrl: string;
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
