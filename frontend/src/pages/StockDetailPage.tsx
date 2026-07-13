import { useParams, useNavigate, Link } from 'react-router-dom';
import { Container, Box, Button } from '@mui/material';
import { ArrowLeft } from 'lucide-react';
import { useStockDetailsQuery } from '@/features/stock-details/model/useStockDetailsQuery';
import StockDetailsView from '@/features/stock-details/ui/StockDetailsView';
import { StateError, TableSkeleton } from '@/shared/components';

/**
 * Страница детальной информации об акции.
 *
 * Маршрут: `/stocks/:ticker`. Тикер читается из URL, данные акции
 * подгружаются через `useStockDetailsQuery`. Позиция пользователя и
 * торговые диалоги рендерятся внутри `StockDetailsView`.
 *
 * Состояния:
 * - loading → TableSkeleton;
 * - сетевая ошибка → StateError с Retry (refetch);
 * - акция не найдена → StateError «Stock not found» с переходом в каталог;
 * - успех → StockDetailsView.
 */
export default function StockDetailPage() {
  const { ticker = '' } = useParams<{ ticker: string }>();
  const navigate = useNavigate();
  const { data: stock, isLoading, isError, error, refetch } = useStockDetailsQuery(ticker);

  return (
    <Container maxWidth="lg">
      <Box sx={{ mt: 4, mb: 3 }}>
        <Button component={Link} to="/stocks" startIcon={<ArrowLeft size={16} />}>
          Назад к акциям
        </Button>
      </Box>

      {isLoading && <TableSkeleton />}

      {isError && <StateError title="Не удалось загрузить акцию" error={error} onRetry={refetch} />}

      {!isLoading && !isError && !stock && (
        <StateError
          title="Акция не найдена."
          helperText={`Нет акции с тикером «${ticker}».`}
          onRetry={() => navigate('/stocks')}
          retryText="Назад к акциям"
        />
      )}

      {stock && <StockDetailsView stock={stock} />}
    </Container>
  );
}
