import { useQuery } from '@tanstack/react-query';
import { getAccount } from '../api/accountApi';
import { AccountResponse } from '../types/accountTypes';
import { queryKeys } from '@/shared/lib/queryKeys';

export function useAccountQuery() {
  return useQuery<AccountResponse, Error>({
    queryKey: queryKeys.account,
    queryFn: getAccount,
  });
}
