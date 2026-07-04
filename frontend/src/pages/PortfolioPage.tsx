import { Container, Typography, Box, Button, Skeleton } from '@mui/material';
import { AlertCircle, RefreshCw } from 'lucide-react';
import { usePortfolioQuery } from '@/features/portfolio/model/usePortfolioQuery';
import PortfolioTable from '@/features/portfolio/ui/PortfolioTable';

export default function PortfolioPage() {
  const { data, isLoading, isError, error, refetch } = usePortfolioQuery();

  return (
    <Container maxWidth="lg">
      <Box sx={{ mt: 4, mb: 3 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          Portfolio
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
            Failed to load portfolio
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
            Your portfolio is empty.
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Buy your first stock on the Stocks page.
          </Typography>
        </Box>
      )}

      {data && data.length > 0 && <PortfolioTable positions={data} />}
    </Container>
  );
}
