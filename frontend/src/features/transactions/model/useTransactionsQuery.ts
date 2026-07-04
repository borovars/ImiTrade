import { useQuery } from '@tanstack/react-query';
import { getTransactions } from '../api/transactionsApi';
import { TransactionPage } from '../types/transactionsTypes';
import { queryKeys } from '@/shared/lib/queryKeys';

/**
 * История операций (server state через React Query, без локального кеша).
 *
 * Ключ queryKeys.transactions уже инвалидируется trading-мутациями
 * (buy/sell), поэтому после сделки таблица обновляется автоматически —
 * ручной state-sync не нужен.
 */
export function useTransactionsQuery() {
  return useQuery<TransactionPage, Error>({
    queryKey: queryKeys.transactions,
    queryFn: getTransactions,
    staleTime: 5 * 60 * 1000,
    refetchOnWindowFocus: false,
    retry: 1,
  });
}
