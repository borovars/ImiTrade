import { useState } from 'react';
import { Container, Typography, Box } from '@mui/material';
import { useStocksQuery } from '@/features/stocks/model/useStocksQuery';
import StocksTable from '@/features/stocks/ui/StocksTable';
import { StateError, StateEmpty, TableSkeleton } from '@/shared/components';

const DEFAULT_ROWS_PER_PAGE = 20;

export default function StocksPage() {
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(DEFAULT_ROWS_PER_PAGE);
  const { data, isLoading, isError, error, refetch, isPlaceholderData } = useStocksQuery(
    page,
    rowsPerPage
  );

  const handlePageChange = (newPage: number) => setPage(newPage);

  const handleRowsPerPageChange = (newSize: number) => {
    setRowsPerPage(newSize);
    setPage(0);
  };

  return (
    <Container maxWidth="lg">
      <Box sx={{ mt: 4, mb: 3 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          Stocks
        </Typography>
      </Box>

      {isLoading && <TableSkeleton />}

      {isError && <StateError title="Failed to load stocks" error={error} onRetry={refetch} />}

      {data && data.content.length === 0 && !isLoading && !isError && (
        <StateEmpty title="No stocks available" />
      )}

      {data && data.content.length > 0 && (
        <StocksTable
          stocks={data.content}
          page={data.number}
          rowsPerPage={data.size}
          totalElements={data.totalElements}
          loading={isPlaceholderData}
          onPageChange={handlePageChange}
          onRowsPerPageChange={handleRowsPerPageChange}
        />
      )}
    </Container>
  );
}
