import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { buyStock } from '../api/tradingApi';
import { BuyStockRequest, TradeResponse } from '../types/tradeTypes';
import { queryKeys } from '@/shared/lib/queryKeys';
import { ApiError } from '@/shared/api/apiClient';

/**
 * Покупка акции.
 *
 * После успеха автоматически инвалидирует account, portfolio, transactions и
 * stocks — без ручного обновления состояния. Каталог акций (stocks)
 * инвалидируется prefix-ключом: это захватывает и список акций, и detail-запрос
 * по тикеру (`['stocks','detail',ticker]`), чтобы страница Stock Detail
 * обновлялась после сделки (хотя сама рыночная цена от сделки не меняется —
 * её обновляет планировщик backend). Toast успеха показывается здесь (уровень
 * хука), ошибки backend — в onError (apiClient уже показывает 403/5xx, здесь
 * добавляем текст для 4xx торговых ошибок).
 */
export function useBuyStockMutation() {
  const queryClient = useQueryClient();

  return useMutation<TradeResponse, ApiError, BuyStockRequest>({
    mutationFn: buyStock,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.account });
      queryClient.invalidateQueries({ queryKey: queryKeys.stocks });
      queryClient.invalidateQueries({ queryKey: queryKeys.portfolio });
      queryClient.invalidateQueries({ queryKey: queryKeys.transactions });
      toast.success(`Bought ${data.quantity} ${data.stockTicker}`);
    },
    onError: (error) => {
      toast.error(error.message);
    },
  });
}
