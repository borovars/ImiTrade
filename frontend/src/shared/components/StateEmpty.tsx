import { Box, Typography } from '@mui/material';

interface StateEmptyProps {
  /** Основной текст empty-состояния, например «Your portfolio is empty.». */
  title: string;
  /** Необязательный поясняющий текст под заголовком. */
  helperText?: string;
}

/**
 * Единый empty-state для всех фич.
 *
 * Применяется во всех списках/таблицах при отсутствии данных: Stocks,
 * Portfolio, Transactions, Account. Не дублировать inline-копии.
 */
export default function StateEmpty({ title, helperText }: StateEmptyProps) {
  return (
    <Box sx={{ textAlign: 'center', py: 6 }}>
      <Typography variant="h6" color="text.secondary">
        {title}
      </Typography>
      {helperText && (
        <Typography variant="body2" color="text.secondary">
          {helperText}
        </Typography>
      )}
    </Box>
  );
}
