import { Container, Typography, Box } from '@mui/material';
import { useTransactionsQuery } from '@/features/transactions/model/useTransactionsQuery';
import TransactionsTable from '@/features/transactions/ui/TransactionsTable';
import { StateError, StateEmpty, TableSkeleton } from '@/shared/components';

export default function TransactionsPage() {
  const { data, isLoading, isError, error, refetch } = useTransactionsQuery();

  return (
    <Container maxWidth="lg">
      <Box sx={{ mt: 4, mb: 3 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          Transactions
        </Typography>
      </Box>

      {isLoading && <TableSkeleton />}

      {isError && (
        <StateError title="Failed to load transactions" error={error} onRetry={refetch} />
      )}

      {data && data.content.length === 0 && !isLoading && !isError && (
        <StateEmpty
          title="No transactions yet."
          helperText="Start trading stocks to see history here."
        />
      )}

      {data && data.content.length > 0 && <TransactionsTable transactions={data.content} />}
    </Container>
  );
}
