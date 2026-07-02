import { useQuery } from '@tanstack/react-query';
import { getStocks } from '../api/stocksApi';
import { Stock } from '../types/stockTypes';

const STOCKS_QUERY_KEY = ['stocks'] as const;

export function useStocksQuery() {
  return useQuery<Stock[], Error>({
    queryKey: STOCKS_QUERY_KEY,
    queryFn: getStocks,
    staleTime: 5 * 60 * 1000,
    refetchOnWindowFocus: false,
    retry: 1,
  });
}
