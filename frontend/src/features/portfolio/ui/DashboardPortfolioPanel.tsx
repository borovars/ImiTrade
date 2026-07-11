import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Box,
  Typography,
} from '@mui/material';
import { PortfolioPosition } from '../types/portfolioTypes';
import { formatMoney, formatProfitLoss, formatPercent } from '@/shared/utils/format';
import { TableSkeleton, StateError, StateEmpty } from '@/shared/components';

interface DashboardPortfolioPanelProps {
  data?: PortfolioPosition[];
  isLoading: boolean;
  isError: boolean;
  error?: Error | null;
  refetch: () => void;
}

/**
 * Read-only панель содержимого портфеля для главной (Dashboard).
 *
 * Показывает позиции пользователя: тикер, компания, количество, среднюю цену
 * покупки, текущую цену, изменение цены (абсолют + процент к средней) и
 * прибыль/убыток. Без кнопок действий — Dashboard остаётся сводным обзором;
 * торговля ведётся со страницы «Портфель» (PortfolioTable + SellStockDialog).
 *
 * Финансовые показатели берёт из backend (pnl уже рассчитан). Изменение цены и
 * стоимость позиции — единственные производные display-значения из примитивов
 * backend (currentPrice, averagePrice, quantity), а не пересчёт PnL/агрегатов.
 */
export default function DashboardPortfolioPanel({
  data,
  isLoading,
  isError,
  error,
  refetch,
}: DashboardPortfolioPanelProps) {
  if (isLoading) {
    return <TableSkeleton />;
  }

  if (isError) {
    return <StateError title="Не удалось загрузить портфель" error={error} onRetry={refetch} />;
  }

  if (!data || data.length === 0) {
    return (
      <StateEmpty
        title="Ваш портфель пуст."
        helperText="Купите первую акцию на странице «Акции»."
      />
    );
  }

  return (
    <Box>
      <Box sx={{ mb: 2 }}>
        <Typography variant="h6" component="h2">
          Содержимое портфеля
        </Typography>
      </Box>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Тикер</TableCell>
              <TableCell>Компания</TableCell>
              <TableCell align="right">Количество</TableCell>
              <TableCell align="right">Ср. цена</TableCell>
              <TableCell align="right">Текущая цена</TableCell>
              <TableCell align="right">Изменение цены</TableCell>
              <TableCell align="right">Прибыль / Убыток</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {data.map((position) => {
              const profitLoss = formatProfitLoss(position.pnl);
              const priceChange = position.currentPrice - position.averagePrice;
              const percent =
                position.averagePrice > 0
                  ? formatPercent((priceChange / position.averagePrice) * 100)
                  : formatPercent(0);
              const priceChangeAbs = formatProfitLoss(priceChange);
              const lotsLabel =
                position.lotSize > 0 && position.quantity % position.lotSize === 0
                  ? ` (${position.quantity / position.lotSize} лот.)`
                  : '';

              return (
                <TableRow key={position.stockId} hover>
                  <TableCell sx={{ fontWeight: 600 }}>{position.ticker}</TableCell>
                  <TableCell>{position.companyName}</TableCell>
                  <TableCell align="right">
                    {position.quantity}
                    {lotsLabel && (
                      <Typography variant="body2" color="text.secondary">
                        {lotsLabel.trim()}
                      </Typography>
                    )}
                  </TableCell>
                  <TableCell align="right">{formatMoney(position.averagePrice)}</TableCell>
                  <TableCell align="right">{formatMoney(position.currentPrice)}</TableCell>
                  <TableCell align="right">
                    <Typography
                      component="span"
                      sx={{ color: percent.color, fontWeight: 600 }}
                    >
                      {priceChangeAbs.text}
                    </Typography>
                    <Typography
                      component="span"
                      variant="body2"
                      sx={{ color: percent.color, ml: 0.75 }}
                    >
                      ({percent.text})
                    </Typography>
                  </TableCell>
                  <TableCell align="right">
                    <Typography component="span" sx={{ color: profitLoss.color, fontWeight: 600 }}>
                      {profitLoss.text}
                    </Typography>
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  );
}
