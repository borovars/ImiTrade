import apiClient from '@/shared/api/apiClient';
import { API_ENDPOINTS } from '@/shared/api/endpoints';
import { HistoryPeriodCode } from '@/shared/lib/chart/periods';
import type { PortfolioHistoryPoint, PortfolioHistoryPointDto } from '../types/portfolioHistoryTypes';

/**
 * Получение истории стоимости портфеля.
 *
 * Backend: `GET /api/v1/portfolio/history?period=` (см. `PortfolioController`).
 * Возвращает готовый временной ряд рыночной стоимости всех когда-либо
 * принадлежавших пользователю позиций (`Σ quantity_held × close_price`).
 * Вся бизнес-логика реконструкции (replay транзакций + исторические цены из
 * MOEX) — на backend; фронт только отображает ряд.
 *
 * `from` не передаём: график стоимости портфеля рисует один диапазон на период
 * (кнопки 1D/1W/1M/1Y), lazy scroll-to-past не используется.
 *
 * Auth-токен (JWT или X-Guest-Token) подставляется интерсептором `apiClient`.
 *
 * @param period  код периода (`1D`/`1W`/`1M`/`1Y`) — задаёт интервал свечи
 * @returns точки `{ time, value }`, отсортированные по времени (backend уже
 *          сортирует по возрастанию `time`)
 */
export async function getPortfolioHistory(period: HistoryPeriodCode): Promise<PortfolioHistoryPoint[]> {
  const response = await apiClient.get<PortfolioHistoryPointDto[]>(
    API_ENDPOINTS.PORTFOLIO.HISTORY,
    { params: { period } }
  );
  return response.data.map(toHistoryPoint);
}

function toHistoryPoint(point: PortfolioHistoryPointDto): PortfolioHistoryPoint {
  return { time: point.time, value: Number(point.value) };
}
