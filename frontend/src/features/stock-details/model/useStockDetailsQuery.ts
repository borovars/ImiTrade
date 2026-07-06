import { useQuery } from '@tanstack/react-query';
import { getStockByTicker } from '../api/stockDetailsApi';
import { Stock } from '@/features/stocks/types/stockTypes';
import { queryKeys } from '@/shared/lib/queryKeys';

/**
 * Загрузка детальной информации об акции по тикеру.
 *
 * Возвращает `Stock | null`: `null` означает, что акция не найдена
 * (страница покажет Error State «Stock not found»). Опции React Query
 * (`staleTime`/`refetchOnWindowFocus`/`retry`) не дублируем — они заданы
 * как дефолты `QueryClient`.
 */
export function useStockDetailsQuery(ticker: string) {
  return useQuery<Stock | null, Error>({
    queryKey: queryKeys.stockDetails(ticker),
    queryFn: () => getStockByTicker(ticker),
  });
}
