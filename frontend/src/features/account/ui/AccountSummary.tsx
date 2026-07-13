import { Card, CardContent, Grid, Skeleton, Box, Typography, Tooltip } from '@mui/material';
import { Wallet, Briefcase, TrendingUp, TrendingDown, Layers, Info } from 'lucide-react';
import { ReactNode } from 'react';
import { formatMoney, formatProfitLoss } from '@/shared/utils/format';
import { StateError, StateEmpty } from '@/shared/components';
import { useAuth } from '@/features/auth/model/authStore';
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
  /** Текст всплывающей подсказки над info-иконкой. Если не передан — иконки нет. */
  infoText?: string;
}

function StatCard({ icon, label, value, color = 'text.primary', infoText }: StatCardProps) {
  return (
    <Card sx={{ position: 'relative' }}>
      <CardContent>
        {infoText && (
          <Box sx={{ position: 'absolute', top: 8, right: 8, color: 'text.secondary' }}>
            <Tooltip title={infoText} arrow placement="bottom-end">
              <Info size={16} />
            </Tooltip>
          </Box>
        )}
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

export default function AccountSummary({
  data,
  isLoading,
  isError,
  error,
  refetch,
}: AccountSummaryProps) {
  const { state } = useAuth();
  const isGuest = state.userType !== 'auth';

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
      <StateError title="Не удалось загрузить данные аккаунта" error={error} onRetry={refetch} />
    );
  }

  if (!data) {
    return <StateEmpty title="Нет данных по аккаунту" />;
  }

  const profitLoss = formatProfitLoss(data.profitLoss);

  return (
    <Grid container spacing={3}>
      <Grid size={{ xs: 12, sm: 6, md: 3 }}>
        <StatCard
          icon={<Wallet size={24} />}
          label="Баланс"
          value={formatMoney(data.balance)}
          infoText={
            isGuest ? 'Чтобы получить ещё +20 000 ᛔ к балансу — зарегистрируйтесь!' : undefined
          }
        />
      </Grid>
      <Grid size={{ xs: 12, sm: 6, md: 3 }}>
        <StatCard
          icon={<Briefcase size={24} />}
          label="Стоимость портфеля"
          value={formatMoney(data.portfolioValue)}
        />
      </Grid>
      <Grid size={{ xs: 12, sm: 6, md: 3 }}>
        <StatCard
          icon={data.profitLoss >= 0 ? <TrendingUp size={24} /> : <TrendingDown size={24} />}
          label="Прибыль / Убыток"
          value={profitLoss.text}
          color={profitLoss.color}
        />
      </Grid>
      <Grid size={{ xs: 12, sm: 6, md: 3 }}>
        <StatCard
          icon={<Layers size={24} />}
          label="Позиции"
          value={String(data.positionsCount)}
        />
      </Grid>
    </Grid>
  );
}
