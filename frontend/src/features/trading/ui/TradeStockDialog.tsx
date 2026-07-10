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
import { lotsSchema, LotsForm } from '../lib/tradeSchema';

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
  /** Вызывается с валидным целым количеством лотов > 0. */
  onSubmit: (lots: number) => void;
}

/**
 * Базовая форма сделки (общая для покупки и продажи).
 *
 * Презентационный компонент: не знает про хуки/мутации, только валидирует
 * число лотов и показывает итоговое количество акций и стоимость.
 * Владелец (Buy/Sell-обёртка) передаёт колбэк сабмита и флаг загрузки.
 *
 * Пользователь вводит лоты; фактическое число акций = `lots × stock.lotSize`
 * (отображается тут же для наглядности, но на backend отправляются только лоты).
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
  } = useForm<LotsForm>({
    resolver: zodResolver(lotsSchema),
    defaultValues: { lots: undefined as unknown as number },
  });

  // Сбрасываем форму при каждом открытии нового диалога.
  useEffect(() => {
    if (open) {
      reset({ lots: undefined as unknown as number });
    }
  }, [open, reset]);

  const watchedLots = useWatch({ control, name: 'lots' });
  const lots = Number(watchedLots) || 0;
  const shares = lots > 0 ? lots * stock.lotSize : 0;
  const total = shares * stock.currentPrice;

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <form onSubmit={handleSubmit(({ lots }) => onSubmit(lots))}>
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
                Текущая цена
              </Typography>
              <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                {formatMoney(stock.currentPrice)}
              </Typography>
            </Box>

            <TextField
              label="Лоты"
              type="number"
              autoFocus
              fullWidth
              error={!!errors.lots}
              helperText={errors.lots?.message ?? `1 лот = ${stock.lotSize} акций`}
              inputProps={{ min: 1, step: 1 }}
              {...register('lots')}
            />

            {shares > 0 && (
              <Typography variant="body2" color="text.secondary">
                {actionLabel === 'Купить' ? 'Вы покупаете:' : 'Вы продаёте:'}{' '}
                <Typography component="span" sx={{ fontWeight: 600 }}>
                  {shares} акций
                </Typography>
              </Typography>
            )}

            <Box
              sx={{
                py: 1.5,
                px: 2,
                bgcolor: 'action.selected',
                borderRadius: 1,
              }}
            >
              <Typography variant="body2" color="text.secondary">
                Итоговая сумма
              </Typography>
              <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                {formatMoney(total)}
              </Typography>
            </Box>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose} disabled={isPending}>
            Отмена
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
