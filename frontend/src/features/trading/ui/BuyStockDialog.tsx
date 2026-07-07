import { Stock } from '@/features/stocks/types/stockTypes';
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

  return (
    <TradeStockDialog
      stock={stock}
      open={open}
      onClose={onClose}
      title="Buy stock"
      actionLabel="Buy"
      actionColor="success"
      isPending={isPending}
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
