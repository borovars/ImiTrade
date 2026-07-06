import { Box, Typography, Button } from '@mui/material';
import { AlertCircle, RefreshCw } from 'lucide-react';

interface StateErrorProps {
  /** Заголовок блока ошибки, например «Failed to load portfolio». */
  title: string;
  /** Текст-плейсхолдер, если у ошибки нет message. По умолчанию «Something went wrong». */
  helperText?: string;
  /** Ошибка из React Query; её message показывается под заголовком. */
  error?: Error | null;
  /** Колбэк кнопки «Retry» — обычно `refetch`. */
  onRetry: () => void;
  /** Текст кнопки. По умолчанию «Retry». */
  retryText?: string;
}

/**
 * Единый блок ошибки для всех фич.
 *
 * Иконка и текст используют цвета темы MUI (error.main), жёстко цвет не
 * прописывается. Применяется во всех списках/таблицах: Stocks, Portfolio,
 * Transactions, Account. Не дублировать inline-копии.
 */
export default function StateError({
  title,
  helperText = 'Something went wrong',
  error,
  onRetry,
  retryText = 'Retry',
}: StateErrorProps) {
  return (
    <Box sx={{ textAlign: 'center', py: 6 }}>
      <Box sx={{ color: 'error.main', mb: 2, display: 'flex', justifyContent: 'center' }}>
        <AlertCircle size={48} />
      </Box>
      <Typography variant="h6" color="error" gutterBottom>
        {title}
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        {error?.message || helperText}
      </Typography>
      <Button variant="outlined" startIcon={<RefreshCw size={16} />} onClick={onRetry}>
        {retryText}
      </Button>
    </Box>
  );
}
