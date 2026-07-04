import { Box, Skeleton } from '@mui/material';

interface TableSkeletonProps {
  /** Число строк-плейсхолдеров под шапкой. По умолчанию 3. */
  rows?: number;
}

/**
 * Единый скелетон загрузки табличных данных.
 *
 * Первая строка выше (56px) имитирует шапку таблицы, остальные (48px) —
 * строки тела. Применяется в Stocks, Portfolio, Transactions. Не дублировать
 * inline-копии `<Skeleton>`.
 */
export default function TableSkeleton({ rows = 3 }: TableSkeletonProps) {
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      <Skeleton variant="rectangular" height={56} />
      {Array.from({ length: rows }).map((_, i) => (
        <Skeleton key={i} variant="rectangular" height={48} />
      ))}
    </Box>
  );
}
