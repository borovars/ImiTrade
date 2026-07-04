import { Container, Typography, Box, Button, Skeleton } from '@mui/material';
import { AlertCircle, RefreshCw } from 'lucide-react';
import { useTransactionsQuery } from '@/features/transactions/model/useTransactionsQuery';
import TransactionsTable from '@/features/transactions/ui/TransactionsTable';

export default function TransactionsPage() {
  const { data, isLoading, isError, error, refetch } = useTransactionsQuery();

  return (
    <Container maxWidth="lg">
      <Box sx={{ mt: 4, mb: 3 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          Transactions
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
            Failed to load transactions
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

      {data && data.content.length === 0 && !isLoading && !isError && (
        <Box sx={{ textAlign: 'center', py: 6 }}>
          <Typography variant="h6" color="text.secondary">
            No transactions yet.
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Start trading stocks to see history here.
          </Typography>
        </Box>
      )}

      {data && data.content.length > 0 && <TransactionsTable transactions={data.content} />}
    </Container>
  );
}
