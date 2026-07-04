import { Card, CardContent, Grid, Skeleton, Box, Typography, Button } from '@mui/material';
import {
  Wallet,
  Briefcase,
  TrendingUp,
  TrendingDown,
  Layers,
  AlertCircle,
  RefreshCw,
} from 'lucide-react';
import { ReactNode } from 'react';
import { formatMoney } from '@/shared/utils/format';
import { AccountResponse } from '../types/accountTypes';

interface AccountSummaryProps {
  data?: AccountResponse;
  isLoading: boolean;
  isError: boolean;
  error?: Error | null;
  refetch: () => void;
}

interface StatCardProps {
  icon: ReactNode;
  label: string;
  value: string;
  color?: string;
}

function StatCard({ icon, label, value, color = 'text.primary' }: StatCardProps) {
  return (
    <Card>
      <CardContent>
        <Box sx={{ color: 'text.secondary', mb: 1.5 }}>{icon}</Box>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 0.5 }}>
          {label}
        </Typography>
        <Typography variant="h5" component="p" sx={{ fontWeight: 600, color }}>
          {value}
        </Typography>
      </CardContent>
    </Card>
  );
}

/** Profit/Loss со знаком и цветом: + зелёный, − красный, 0 нейтрально. */
function formatProfitLoss(value: number): { text: string; color: string } {
  if (value > 0) {
    return { text: `+ ${formatMoney(value)}`, color: 'success.main' };
  }
  if (value < 0) {
    return { text: `− ${formatMoney(Math.abs(value))}`, color: 'error.main' };
  }
  return { text: formatMoney(value), color: 'text.primary' };
}

export default function AccountSummary({
  data,
  isLoading,
  isError,
  error,
  refetch,
}: AccountSummaryProps) {
  if (isLoading) {
    return (
      <Grid container spacing={3}>
        {[0, 1, 2, 3].map((i) => (
          <Grid key={i} size={{ xs: 12, sm: 6, md: 3 }}>
            <Skeleton variant="rectangular" height={140} sx={{ borderRadius: 1 }} />
          </Grid>
        ))}
      </Grid>
    );
  }

  if (isError) {
    return (
      <Box sx={{ textAlign: 'center', py: 6 }}>
        <AlertCircle size={48} color="#d32f2f" style={{ marginBottom: 16 }} />
        <Typography variant="h6" color="error" gutterBottom>
          Failed to load account data
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          {error?.message || 'Something went wrong'}
        </Typography>
        <Button variant="outlined" startIcon={<RefreshCw size={16} />} onClick={() => refetch()}>
          Retry
        </Button>
      </Box>
    );
  }

  if (!data) {
    return (
      <Box sx={{ textAlign: 'center', py: 6 }}>
        <Typography variant="h6" color="text.secondary">
          No account data available
        </Typography>
      </Box>
    );
  }

  const profitLoss = formatProfitLoss(data.profitLoss);

  return (
    <Grid container spacing={3}>
      <Grid size={{ xs: 12, sm: 6, md: 3 }}>
        <StatCard
          icon={<Wallet size={24} />}
          label="Balance"
          value={formatMoney(data.balance)}
        />
      </Grid>
      <Grid size={{ xs: 12, sm: 6, md: 3 }}>
        <StatCard
          icon={<Briefcase size={24} />}
          label="Portfolio Value"
          value={formatMoney(data.portfolioValue)}
        />
      </Grid>
      <Grid size={{ xs: 12, sm: 6, md: 3 }}>
        <StatCard
          icon={data.profitLoss >= 0 ? <TrendingUp size={24} /> : <TrendingDown size={24} />}
          label="Profit / Loss"
          value={profitLoss.text}
          color={profitLoss.color}
        />
      </Grid>
      <Grid size={{ xs: 12, sm: 6, md: 3 }}>
        <StatCard
          icon={<Layers size={24} />}
          label="Positions"
          value={String(data.positionsCount)}
        />
      </Grid>
    </Grid>
  );
}
