import { Box, Button, Card, CardContent, Container, Typography } from '@mui/material';
import { Link } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { usePortfolioQuery } from '@/features/portfolio/model/usePortfolioQuery';
import PortfolioTable from '@/features/portfolio/ui/PortfolioTable';
import PortfolioValueChart from '@/features/portfolio/ui/PortfolioValueChart';
import { StateError, TableSkeleton } from '@/shared/components';

export default function PortfolioPage() {
  const { data, isLoading, isError, error, refetch } = usePortfolioQuery();

  return (
    <Container maxWidth="lg">
      <Box sx={{ mt: 4, mb: 3 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          Портфель
        </Typography>
      </Box>

      {isLoading && <TableSkeleton />}

      {isError && (
        <StateError title="Не удалось загрузить портфель" error={error} onRetry={refetch} />
      )}

      {data && data.length === 0 && !isLoading && !isError && (
        <EmptyPortfolioState />
      )}

      {data && data.length > 0 && (
        <>
          <Card sx={{ mb: 3 }}>
            <CardContent>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 1, px: 1 }}>
                Стоимость портфеля
              </Typography>
              <PortfolioValueChart />
            </CardContent>
          </Card>
          <PortfolioTable positions={data} />
        </>
      )}
    </Container>
  );
}

/**
 * Empty-state пустого портфеля с CTA на страницу акций.
 *
 * ТЗ «Пустой портфель»: не показываем пустой график/таблицу — вместо этого
 * поясняем, что стоимость начнёт отслеживаться после первой покупки, и даём
 * кнопку перехода в каталог акций. Инлайнится здесь (а не расширяет общий
 * `StateEmpty`, у которого намеренно нет action-пропа).
 */
function EmptyPortfolioState() {
  return (
    <Box
      sx={{
        textAlign: 'center',
        py: 6,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 2,
      }}
    >
      <Box>
        <Typography variant="h6" gutterBottom>
          Ваш портфель пока пуст.
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Купите первую акцию, чтобы начать отслеживать изменение стоимости портфеля.
        </Typography>
      </Box>
      <Button
        component={Link}
        to="/stocks"
        variant="contained"
        startIcon={<ArrowLeft size={16} />}
        sx={{
          bgcolor: '#3dba8d',
          color: '#fff',
          '&:hover': { bgcolor: '#35a57d' },
        }}
      >
        Перейти к акциям
      </Button>
    </Box>
  );
}
