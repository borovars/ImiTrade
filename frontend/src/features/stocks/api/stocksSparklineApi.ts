import apiClient from '@/shared/api/apiClient';
import { API_ENDPOINTS } from '@/shared/api/endpoints';
import { SparklinePoint } from '../types/sparklineTypes';

/**
 * Мини-история цены акции для sparkline в строке каталога.
 *
 * Источник — `GET /api/v1/stocks/{ticker}/history` (тот же, что у большого
 * графика Stock Detail). Берём ~1 год (`period=1M` = месячные свечи, lookback
 * ~3 года на backend; фронт ограничится последним годом в `toSparklineData`),
 * но рисуем только `close`. OHLCV-контракт не протекает в UI.
 *
 * `period='1M'` даёт месячные свечи — за год это ~12 точек: достаточно для
 * формы линии без лишней нагрузки на MOEX (один тикер = один запрос).
 *
 * `from` (ISO `yyyy-MM-dd`) — левая граница; для года это «сегодня − 365 дней».
 */
export async function getStockSparkline(ticker: string, from: Date): Promise<SparklinePoint[]> {
  const response = await apiClient.get<{ time: string; close: number | string }[]>(
    API_ENDPOINTS.STOCKS.history(ticker),
    { params: { period: '1M', from: toIsoDate(from) } }
  );
  return response.data
    .filter((c) => c.close != null)
    .map((c) => ({ time: c.time, close: Number(c.close) }));
}

function toIsoDate(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}
