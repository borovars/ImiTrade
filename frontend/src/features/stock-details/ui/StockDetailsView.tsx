import { useState } from 'react';
import { Box, Typography, Paper, Chip, Grid, Button, Skeleton } from '@mui/material';
import { TrendingUp } from 'lucide-react';
import { Stock } from '@/features/stocks/types/stockTypes';
import { usePortfolioQuery } from '@/features/portfolio/model/usePortfolioQuery';
import { formatMoney, formatProfitLoss } from '@/shared/utils/format';
import BuyStockDialog from '@/features/trading/ui/BuyStockDialog';
import SellStockDialog from '@/features/trading/ui/SellStockDialog';

type TradeMode = 'buy' | 'sell';

interface StockDetailsViewProps {
  stock: Stock;
}

/**
 * Контент страницы детальной информации об акции.
 *
 * Блоки:
 * - Header: компания, тикер, биржа;
 * - Price Block: текущая рыночная цена крупным шрифтом;
 * - User Position: позиция пользователя (если есть) с PnL;
 * - Trading: кнопки Buy/Sell, открывающие существующие диалоги;
 * - Company Information: доступные поля об акции.
 *
 * Позиция пользователя берётся через `usePortfolioQuery` (фильтрация по
 * `stockId` локально). PnL уже рассчитан на backend. Buy/Sell переиспользуют
 * `BuyStockDialog`/`SellStockDialog` из features/trading — новая логика
 * торговли не реализуется.
 */
export default function StockDetailsView({ stock }: StockDetailsViewProps) {
  const { data: positions, isLoading: isPortfolioLoading } = usePortfolioQuery();
  const [tradeMode, setTradeMode] = useState<TradeMode | null>(null);

  const closeTrade = () => setTradeMode(null);
  const position = positions?.find((p) => p.stockId === stock.id) ?? null;

  return (
    <Box>
      {/* Header */}
      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          {stock.companyName}
        </Typography>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap' }}>
          <Chip label={stock.ticker} color="primary" />
          <Chip label={stock.exchange} variant="outlined" />
        </Box>
      </Paper>

      <Grid container spacing={3}>
        {/* Price Block */}
        <Grid size={{ xs: 12, md: 6 }}>
          <Paper sx={{ p: 3, height: '100%' }}>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
              Current Price
            </Typography>
            <Box sx={{ display: 'flex', alignItems: 'baseline', gap: 1 }}>
              <Box component={TrendingUp} sx={{ color: 'success.main', alignSelf: 'center' }} size={32} />
              <Typography variant="h3" component="span" sx={{ fontWeight: 700 }}>
                {formatMoney(stock.currentPrice)}
              </Typography>
            </Box>
          </Paper>
        </Grid>

        {/* User Position */}
        <Grid size={{ xs: 12, md: 6 }}>
          <Paper sx={{ p: 3, height: '100%' }}>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
              Your Position
            </Typography>
            {isPortfolioLoading ? (
              <Skeleton variant="rectangular" height={80} />
            ) : position ? (
              <PositionDetails
                quantity={position.quantity}
                lotSize={position.lotSize}
                averagePrice={position.averagePrice}
                currentPrice={position.currentPrice}
                pnl={position.pnl}
              />
            ) : (
              <Typography color="text.secondary">You do not own this stock.</Typography>
            )}
          </Paper>
        </Grid>
      </Grid>

      {/* Trading */}
      <Paper sx={{ p: 3, mt: 3 }}>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Trade
        </Typography>
        <Box sx={{ display: 'flex', gap: 2 }}>
          <Button
            variant="contained"
            color="success"
            onClick={() => setTradeMode('buy')}
          >
            Buy
          </Button>
          <Button
            variant="contained"
            color="error"
            onClick={() => setTradeMode('sell')}
          >
            Sell
          </Button>
        </Box>
      </Paper>

      {/* Company Information */}
      <Paper sx={{ p: 3, mt: 3 }}>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Company Information
        </Typography>
        <Grid container spacing={2}>
          <InfoRow label="Ticker" value={stock.ticker} />
          <InfoRow label="Company Name" value={stock.companyName} />
          <InfoRow label="Exchange" value={stock.exchange} />
          <InfoRow label="Lot Size" value={`${stock.lotSize} shares`} />
        </Grid>
      </Paper>

      {tradeMode === 'buy' && <BuyStockDialog stock={stock} open onClose={closeTrade} />}
      {tradeMode === 'sell' && <SellStockDialog stock={stock} open onClose={closeTrade} />}
    </Box>
  );
}

/**
 * Детали позиции пользователя: количество, средняя цена, текущая стоимость, PnL.
 * PnL берётся из backend, Position Value — единственное производное (display).
 */
function PositionDetails({
  quantity,
  lotSize,
  averagePrice,
  currentPrice,
  pnl,
}: {
  quantity: number;
  lotSize: number;
  averagePrice: number;
  currentPrice: number;
  pnl: number;
}) {
  const positionValue = quantity * currentPrice;
  const profitLoss = formatProfitLoss(pnl);
  const lotsLabel = lotSize > 0 && quantity % lotSize === 0 ? ` (${quantity / lotSize} lots)` : '';

  return (
    <Grid container spacing={2}>
      <InfoRow label="Quantity" value={`${quantity}${lotsLabel}`} />
      <InfoRow label="Average Price" value={formatMoney(averagePrice)} />
      <InfoRow label="Position Value" value={formatMoney(positionValue)} />
      <Grid size={{ xs: 12 }}>
        <Typography variant="body2" color="text.secondary">
          Profit / Loss
        </Typography>
        <Typography component="span" sx={{ color: profitLoss.color, fontWeight: 700 }}>
          {profitLoss.text}
        </Typography>
      </Grid>
    </Grid>
  );
}

function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <Grid size={{ xs: 12, sm: 6 }}>
      <Typography variant="body2" color="text.secondary">
        {label}
      </Typography>
      <Typography sx={{ fontWeight: 600 }}>{value}</Typography>
    </Grid>
  );
}
