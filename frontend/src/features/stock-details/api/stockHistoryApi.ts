import apiClient from '@/shared/api/apiClient';
import { API_ENDPOINTS } from '@/shared/api/endpoints';
import { HistoryPeriodCode } from '@/shared/lib/chart/periods';
import type { HistoryPoint } from '@/shared/lib/chart/historyPoint';
import { HistoryCandleDto } from '../model/historyTypes';

/**
 * Получение истории цены акции.
 *
 * Backend: `GET /api/v1/stocks/{ticker}/history?period=&from=`
 * (см. `StockHistoryController`). Возвращает полный OHLCV (`CandleResponse`),
 * но линейному графику нужны только `time` и `close` — лишние поля отсекаются
 * здесь, чтобы в слои UI не протекал свечной контракт.
 *
 * `from` (ISO `yyyy-MM-dd`) сдвигает левую границу диапазона в прошлое — это
 * и есть механизм lazy scroll-to-past: при прокрутке графика влево фронт
 * запрашивает предыдущий кусок через `from`. Правая граница всегда «сегодня»
 * (`till` = `LocalDate.now()` на backend), поэтому параметр `to` не нужен.
 *
 * Auth-токен (JWT или X-Guest-Token) подставляется интерсептором `apiClient`.
 *
 * @param ticker  тикер, напр. `SBER`
 * @param from    левая граница диапазона; кодируется как `yyyy-MM-dd`
 * @param period  код периода (`1D`/`1W`/`1M`/`1Y`) — задаёт интервал свечи
 * @returns точки `{ time, value }`, отсортированные по времени (backend уже
 *          сортирует по возрастанию `time`)
 */
export async function getStockHistory(
  ticker: string,
  from: Date,
  period: HistoryPeriodCode
): Promise<HistoryPoint[]> {
  const response = await apiClient.get<HistoryCandleDto[]>(
    API_ENDPOINTS.STOCKS.history(ticker),
    { params: { period, from: toIsoDate(from) } }
  );
  return response.data.map(toHistoryPoint);
}

/**
 * ISO-дата `yyyy-MM-dd` для query-параметра `from` — так её парсит
 * `@RequestParam LocalDate from` на backend.
 */
function toIsoDate(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

function toHistoryPoint(candle: HistoryCandleDto): HistoryPoint {
  // Графику нужен только close — кладём его в общее поле `value`, которое
  // одинаково интерпретируется и графиком цены, и графиком стоимости портфеля.
  return { time: candle.time, value: Number(candle.close) };
}
