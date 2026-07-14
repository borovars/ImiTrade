import { Stock } from '@/features/stocks/types/stockTypes';
import { useAccountQuery } from '@/features/account/model/useAccountQuery';
import { useBuyStockMutation } from '../model/useBuyStockMutation';
import TradeStockDialog from './TradeStockDialog';

interface BuyStockDialogProps {
  stock: Stock;
  open: boolean;
  onClose: () => void;
}

/** Диалог покупки акции. */
export default function BuyStockDialog({ stock, open, onClose }: BuyStockDialogProps) {
  const { mutate, isPending } = useBuyStockMutation();
  // Баланс нужен, чтобы показать текущий баланс и баланс после покупки.
  // Тот же ключ React Query, что и в топбаре/dashboard — после сделки
  // мутация инвалидатирует `account`, и значение обновится автоматически.
  const { data: account } = useAccountQuery();

  return (
    <TradeStockDialog
      stock={stock}
      open={open}
      onClose={onClose}
      title="Покупка акции"
      actionLabel="Купить"
      actionColor="success"
      isPending={isPending}
      balance={account?.balance}
      onSubmit={(lots) => {
        mutate(
          { stockId: stock.id, lots },
          {
            onSuccess: () => onClose(),
          }
        );
      }}
    />
  );
}
