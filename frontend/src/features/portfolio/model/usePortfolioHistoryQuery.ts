import { useQuery } from '@tanstack/react-query';
import { getPortfolioHistory } from '../api/portfolioHistoryApi';
import { HistoryPeriodCode } from '@/shared/lib/chart/periods';
import type { PortfolioHistoryPoint } from '../types/portfolioHistoryTypes';
import { queryKeys } from '@/shared/lib/queryKeys';

/**
 * Загрузка истории стоимости портфеля для графика.
 *
 * Ключ строится через общий реестр (`queryKeys.portfolioHistory`) и вкладывается
 * в namespace `portfolio`, поэтому инвалидация `queryKeys.portfolio` (prefix) в
 * trading-мутациях buy/sell захватит и этот запрос — после сделки график
 * обновится автоматически.
 *
 * Опции `staleTime`/`refetchOnWindowFocus`/`retry` не дублируем — они заданы
 * как дефолты `QueryClient` (`queryProvider`).
 */
export function usePortfolioHistoryQuery(period: HistoryPeriodCode) {
  return useQuery<PortfolioHistoryPoint[], Error>({
    queryKey: queryKeys.portfolioHistory(period),
    queryFn: () => getPortfolioHistory(period),
  });
}
