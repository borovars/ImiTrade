import apiClient from '@/shared/api/apiClient';
import { API_ENDPOINTS } from '@/shared/api/endpoints';
import { StockPage } from '../types/stockTypes';

/**
 * Каталог акций (Spring Page, по умолчанию size=20). page — 0-based.
 *
 * `sort` — Spring Data expression,например `ticker,asc` / `currentPrice,desc` /
 * `lotSize,asc`. Backend принимает её через `@PageableDefault Pageable`
 * (`StockController.getStocks`) → `findAll(spec, pageable)`. Дефолт
 * `ticker,asc` задаёт стабильный алфавитный порядок.
 *
 * `search` — свободный поиск по тикеру ИЛИ названию компании (частичное
 * совпадение без учёта регистра, OR-предикат на backend
 * `StockSpecifications.tickerOrCompanyNameContainsIgnoreCase`). Пустая строка
 * фильтр не накладывает.
 */
export async function getStocks(
  page = 0,
  size = 20,
  sort = 'ticker,asc',
  search = ''
): Promise<StockPage> {
  const response = await apiClient.get<StockPage>(API_ENDPOINTS.STOCKS.BASE, {
    params: { page, size, sort, search: search.trim() || undefined },
  });
  return response.data;
}
