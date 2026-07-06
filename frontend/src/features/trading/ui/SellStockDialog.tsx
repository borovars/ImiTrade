import { Stock } from '@/features/stocks/types/stockTypes';
import { useSellStockMutation } from '../model/useSellStockMutation';
import TradeStockDialog from './TradeStockDialog';

interface SellStockDialogProps {
  stock: Stock;
  open: boolean;
  onClose: () => void;
}

/** Диалог продажи акции. Логика аналогична покупке. */
export default function SellStockDialog({ stock, open, onClose }: SellStockDialogProps) {
  const { mutate, isPending } = useSellStockMutation();

  return (
    <TradeStockDialog
      stock={stock}
      open={open}
      onClose={onClose}
      title="Sell stock"
      actionLabel="Sell"
      actionColor="error"
      isPending={isPending}
      onSubmit={(quantity) => {
        mutate(
          { stockId: stock.id, quantity },
          {
            onSuccess: () => onClose(),
          }
        );
      }}
    />
  );
}
