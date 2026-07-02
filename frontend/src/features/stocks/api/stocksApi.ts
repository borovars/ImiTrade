import apiClient from '@/shared/api/apiClient';
import { API_ENDPOINTS } from '@/shared/api/endpoints';
import { Stock } from '../types/stockTypes';

export async function getStocks(): Promise<Stock[]> {
  const response = await apiClient.get<Stock[]>(API_ENDPOINTS.STOCKS.BASE);
  return response.data;
}
