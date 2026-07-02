import { Container, Typography, Box, Button, Skeleton } from '@mui/material';
import { AlertCircle, RefreshCw } from 'lucide-react';
import { useStocksQuery } from '@/features/stocks/model/useStocksQuery';
import StocksTable from '@/features/stocks/ui/StocksTable';

export default function StocksPage() {
  const { data, isLoading, isError, error, refetch } = useStocksQuery();

  return (
    <Container maxWidth="lg">
      <Box sx={{ mt: 4, mb: 3 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          Stocks
        </Typography>
      </Box>

      {isLoading && (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <Skeleton variant="rectangular" height={56} />
          <Skeleton variant="rectangular" height={48} />
          <Skeleton variant="rectangular" height={48} />
          <Skeleton variant="rectangular" height={48} />
        </Box>
      )}

      {isError && (
        <Box sx={{ textAlign: 'center', py: 6 }}>
          <AlertCircle size={48} color="#d32f2f" style={{ marginBottom: 16 }} />
          <Typography variant="h6" color="error" gutterBottom>
            Failed to load stocks
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            {error?.message || 'Something went wrong'}
          </Typography>
          <Button
            variant="outlined"
            startIcon={<RefreshCw size={16} />}
            onClick={() => refetch()}
          >
            Retry
          </Button>
        </Box>
      )}

      {data && data.length === 0 && !isLoading && !isError && (
        <Box sx={{ textAlign: 'center', py: 6 }}>
          <Typography variant="h6" color="text.secondary">
            No stocks available
          </Typography>
        </Box>
      )}

      {data && data.length > 0 && <StocksTable stocks={data} />}
    </Container>
  );
}
