import { useQuery } from '@tanstack/react-query';
import { getStocks } from '../api/stocksApi';
import { Stock } from '../types/stockTypes';
import { queryKeys } from '@/shared/lib/queryKeys';

export function useStocksQuery() {
  return useQuery<Stock[], Error>({
    queryKey: queryKeys.stocks,
    queryFn: getStocks,
    staleTime: 5 * 60 * 1000,
    refetchOnWindowFocus: false,
    retry: 1,
  });
}
