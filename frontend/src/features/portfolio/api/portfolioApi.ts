import apiClient from '@/shared/api/apiClient';
import { API_ENDPOINTS } from '@/shared/api/endpoints';
import { PortfolioPosition } from '../types/portfolioTypes';

/** Текущие позиции пользователя с живыми ценами и расcчитанным на backend pnl. */
export async function getPortfolio(): Promise<PortfolioPosition[]> {
  const response = await apiClient.get<PortfolioPosition[]>(API_ENDPOINTS.PORTFOLIO.BASE);
  // Backend отдаёт массив позиций (List<PortfolioResponse>), разворачиваем axios-ответ.
  return response.data;
}
