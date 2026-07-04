import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { getStocks } from '../api/stocksApi';
import { StockPage } from '../types/stockTypes';
import { queryKeys } from '@/shared/lib/queryKeys';

/**
 * Постраничный каталог акций (server state через React Query).
 *
 * Особый случай пагинации: `placeholderData: keepPreviousData` держит на
 * экране предыдущую страницу, пока грузится новая, — без мигания скелетона.
 * `isPlaceholderData` (из хука) блокирует контролы пагинации на время загрузки.
 * Остальные дефолты QueryClient (staleTime/retry/…) не дублируются.
 */
export function useStocksQuery(page: number, size: number) {
  return useQuery<StockPage, Error>({
    queryKey: [...queryKeys.stocks, { page, size }],
    queryFn: () => getStocks(page, size),
    placeholderData: keepPreviousData,
  });
}
