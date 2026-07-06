import apiClient from '@/shared/api/apiClient';
import { API_ENDPOINTS } from '@/shared/api/endpoints';
import { StockPage } from '../types/stockTypes';

/** Каталог акций (Spring Page, по умолчанию size=20). page — 0-based. */
export async function getStocks(page = 0, size = 20): Promise<StockPage> {
  const response = await apiClient.get<StockPage>(API_ENDPOINTS.STOCKS.BASE, {
    params: { page, size },
  });
  return response.data;
}
