import apiClient from '@/shared/api/apiClient';
import { API_ENDPOINTS } from '@/shared/api/endpoints';
import { Stock } from '../types/stockTypes';

/** Spring Data Page — формат ответа GET /api/v1/stocks. */
interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export async function getStocks(): Promise<Stock[]> {
  const response = await apiClient.get<PageResponse<Stock>>(API_ENDPOINTS.STOCKS.BASE);
  // Backend отдаёт Spring Page (объект с content), отдаём массив акций.
  return response.data.content;
}
