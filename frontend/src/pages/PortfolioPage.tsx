import { Container, Typography, Box } from '@mui/material';
import { usePortfolioQuery } from '@/features/portfolio/model/usePortfolioQuery';
import PortfolioTable from '@/features/portfolio/ui/PortfolioTable';
import { StateError, StateEmpty, TableSkeleton } from '@/shared/components';

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
        <StateEmpty
          title="Ваш портфель пуст."
          helperText="Купите первую акцию на странице «Акции»."
        />
      )}

      {data && data.length > 0 && <PortfolioTable positions={data} />}
    </Container>
  );
}
