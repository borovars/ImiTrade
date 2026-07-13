import apiClient from '@/shared/api/apiClient';
import { API_ENDPOINTS } from '@/shared/api/endpoints';
import { BuyStockRequest, SellStockRequest, TradeResponse } from '../types/tradeTypes';

/** Купить акцию по текущей рыночной цене. */
export async function buyStock(req: BuyStockRequest): Promise<TradeResponse> {
  const response = await apiClient.post<TradeResponse>(API_ENDPOINTS.TRADING.BUY, req);
  return response.data;
}

/** Продать акцию по текущей рыночной цене. */
export async function sellStock(req: SellStockRequest): Promise<TradeResponse> {
  const response = await apiClient.post<TradeResponse>(API_ENDPOINTS.TRADING.SELL, req);
  return response.data;
}
