import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { sellStock } from '../api/tradingApi';
import { SellStockRequest, TradeResponse } from '../types/tradeTypes';
import { queryKeys } from '@/shared/lib/queryKeys';
import { ApiError } from '@/shared/api/apiClient';

/**
 * Продажа акции. Логика аналогична покупке (см. useBuyStockMutation).
 */
export function useSellStockMutation() {
  const queryClient = useQueryClient();

  return useMutation<TradeResponse, ApiError, SellStockRequest>({
    mutationFn: sellStock,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.account });
      queryClient.invalidateQueries({ queryKey: queryKeys.stocks });
      queryClient.invalidateQueries({ queryKey: queryKeys.portfolio });
      queryClient.invalidateQueries({ queryKey: queryKeys.transactions });
      toast.success(`Sold ${data.quantity} ${data.stockTicker}`);
    },
    onError: (error) => {
      toast.error(error.message);
    },
  });
}
