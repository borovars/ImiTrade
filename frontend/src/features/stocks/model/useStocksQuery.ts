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
 *
 * `sort` и `search` включены в query-key — каждая комбинация получает свой кэш,
 * и `keepPreviousData` корректно держит старую страницу при их смене. Сортировка
 * и поиск серверные (backend `@PageableDefault Pageable` + search-предикат),
 * т.к. каталог пагинируется и клиентская обработка переупорядочила/отфильтровала
 * бы только текущую страницу.
 */
export function useStocksQuery(page: number, size: number, sort: string, search: string) {
  return useQuery<StockPage, Error>({
    queryKey: [...queryKeys.stocks, { page, size, sort, search }],
    queryFn: () => getStocks(page, size, sort, search),
    placeholderData: keepPreviousData,
  });
}
