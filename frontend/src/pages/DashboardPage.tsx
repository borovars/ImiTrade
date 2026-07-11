import { Container, Typography, Box } from '@mui/material';
import { useAccountQuery } from '@/features/account/model/useAccountQuery';
import AccountSummary from '@/features/account/ui/AccountSummary';
import { usePortfolioQuery } from '@/features/portfolio/model/usePortfolioQuery';
import DashboardPortfolioPanel from '@/features/portfolio/ui/DashboardPortfolioPanel';

export default function DashboardPage() {
  const { data, isLoading, isError, error, refetch } = useAccountQuery();
  const {
    data: portfolioData,
    isLoading: portfolioLoading,
    isError: portfolioError,
    error: portfolioErr,
    refetch: portfolioRefetch,
  } = usePortfolioQuery();

  return (
    <Container maxWidth="lg">
      <Box sx={{ mt: 4, mb: 3 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          Главная
        </Typography>
      </Box>

      <AccountSummary
        data={data}
        isLoading={isLoading}
        isError={isError}
        error={error}
        refetch={refetch}
      />

      <Box sx={{ mt: 4 }}>
        <DashboardPortfolioPanel
          data={portfolioData}
          isLoading={portfolioLoading}
          isError={portfolioError}
          error={portfolioErr}
          refetch={portfolioRefetch}
        />
      </Box>
    </Container>
  );
}
