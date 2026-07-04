import { useQuery } from '@tanstack/react-query';
import { getAccount } from '../api/accountApi';
import { AccountResponse } from '../types/accountTypes';

const ACCOUNT_QUERY_KEY = ['account'] as const;

export function useAccountQuery() {
  return useQuery<AccountResponse, Error>({
    queryKey: ACCOUNT_QUERY_KEY,
    queryFn: getAccount,
    staleTime: 5 * 60 * 1000,
    refetchOnWindowFocus: false,
    retry: 1,
  });
}
