import { useQuery } from '@tanstack/react-query';
import { getPortfolio } from '../api/portfolioApi';
import { PortfolioPosition } from '../types/portfolioTypes';
import { queryKeys } from '@/shared/lib/queryKeys';

export function usePortfolioQuery() {
  return useQuery<PortfolioPosition[], Error>({
    queryKey: queryKeys.portfolio,
    queryFn: getPortfolio,
  });
}
