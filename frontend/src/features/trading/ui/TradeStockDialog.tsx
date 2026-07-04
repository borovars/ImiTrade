import { useEffect } from 'react';
import { useForm, useWatch } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Typography,
  Box,
  Stack,
} from '@mui/material';
import { Stock } from '@/features/stocks/types/stockTypes';
import { formatMoney } from '@/shared/utils/format';
import { quantitySchema, QuantityForm } from '../lib/tradeSchema';

interface TradeStockDialogProps {
  stock: Stock;
  open: boolean;
  onClose: () => void;
  /** Заголовок и текст кнопки подтверждения. */
  title: string;
  actionLabel: string;
  /** MUI-цвет кнопки подтверждения ('success' / 'error' и т.д.). */
  actionColor?: 'success' | 'error' | 'primary';
  isPending: boolean;
  /** Вызывается с валидным целым quantity > 0. */
  onSubmit: (quantity: number) => void;
}

/**
 * Базовая форма сделки (общая для покупки и продажи).
 *
 * Презентационный компонент: не знает про хуки/мутации, только валидирует
 * количество и считает стоимость. Владелец (Buy/Sell-обёртка) передаёт
 * колбэк сабмита и флаг загрузки.
 */
export default function TradeStockDialog({
  stock,
  open,
  onClose,
  title,
  actionLabel,
  actionColor = 'primary',
  isPending,
  onSubmit,
}: TradeStockDialogProps) {
  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors },
  } = useForm<QuantityForm>({
    resolver: zodResolver(quantitySchema),
    defaultValues: { quantity: undefined as unknown as number },
  });

  // Сбрасываем форму при каждом открытии нового диалога.
  useEffect(() => {
    if (open) {
      reset({ quantity: undefined as unknown as number });
    }
  }, [open, reset]);

  const quantity = useWatch({ control, name: 'quantity' });
  const qty = Number(quantity) || 0;
  const total = qty > 0 ? qty * stock.currentPrice : 0;

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <form onSubmit={handleSubmit(({ quantity }) => onSubmit(quantity))}>
        <DialogTitle>{title}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 0.5 }}>
            <Box>
              <Typography variant="h6" component="p" sx={{ fontWeight: 600 }}>
                {stock.companyName}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {stock.ticker} · {stock.exchange}
              </Typography>
            </Box>

            <Box
              sx={{
                py: 1.5,
                px: 2,
                bgcolor: 'action.selected',
                borderRadius: 1,
              }}
            >
              <Typography variant="body2" color="text.secondary">
                Current price
              </Typography>
              <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                {formatMoney(stock.currentPrice)}
              </Typography>
            </Box>

            <TextField
              label="Quantity"
              type="number"
              autoFocus
              fullWidth
              error={!!errors.quantity}
              helperText={errors.quantity?.message}
              inputProps={{ min: 1, step: 1 }}
              {...register('quantity')}
            />

            <Box
              sx={{
                py: 1.5,
                px: 2,
                bgcolor: 'action.selected',
                borderRadius: 1,
              }}
            >
              <Typography variant="body2" color="text.secondary">
                Estimated total
              </Typography>
              <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                {formatMoney(total)}
              </Typography>
            </Box>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose} disabled={isPending}>
            Cancel
          </Button>
          <Button
            type="submit"
            variant="contained"
            color={actionColor}
            loading={isPending}
          >
            {actionLabel}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
