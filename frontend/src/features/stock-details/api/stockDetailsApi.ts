import apiClient from '@/shared/api/apiClient';
import { API_ENDPOINTS } from '@/shared/api/endpoints';
import { Stock, StockPage } from '@/features/stocks/types/stockTypes';

/**
 * Получение акции по тикеру.
 *
 * Backend не имеет эндпоинта `GET /stocks/{ticker}` — только `GET /stocks/{id}`
 * (числовой id). Поэтому используем существующий фильтр
 * `GET /api/v1/stocks?ticker=<ticker>` (case-insensitive exact match через
 * `StockSpecifications.hasTickerIgnoreCase`), который возвращает Spring Page.
 * Из ответа берём `content[0]`.
 *
 * Возвращает `null`, если акция с таким тикером не найдена, — чтобы вызывающий
 * код мог отличить «не найдено» (Error State «Stock not found») от сетевой
 * ошибки (обычный `StateError` с Retry).
 */
export async function getStockByTicker(ticker: string): Promise<Stock | null> {
  const response = await apiClient.get<StockPage>(API_ENDPOINTS.STOCKS.BASE, {
    params: { ticker },
  });
  return response.data.content[0] ?? null;
}
