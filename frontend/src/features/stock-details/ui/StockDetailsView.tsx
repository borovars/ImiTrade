import { useState } from 'react';
import { Box, Typography, Paper, Chip, Grid, Button, Skeleton, Link, Avatar } from '@mui/material';
import { TrendingUp, Globe, Building2 } from 'lucide-react';
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
 * Базовый URL backend, от которого раздаются статические ресурсы (логотипы).
 * Совпадает с `baseURL` axios-клиента (`apiClient.ts`).
 */
const API_BASE_URL = (import.meta.env.VITE_API_URL as string | undefined) ?? '';

/**
 * Превращает API-relative путь логотипа (`/logos/SBER.svg`, который отдаёт
 * backend) в абсолютный URL для `<img src>`. Префикс `VITE_API_URL` нужен,
 * т.к. фронтенд и backend в dev работают на разных портах.
 */
function resolveLogoUrl(logoUrl: string): string {
  return `${API_BASE_URL}${logoUrl}`;
}

/**
 * Контент страницы детальной информации об акции.
 *
 * Блоки:
 * - Header: логотип, компания, тикер, биржа, сектор;
 * - Price Block: текущая рыночная цена крупным шрифтом;
 * - User Position: позиция пользователя (если есть) с PnL;
 * - Trading: кнопки Buy/Sell, открывающие существующие диалоги;
 * - About: описание компании (из backend);
 * - Company Information: тикер, название, биржа, лотность, сайт.
 *
 * Позиция пользователя берётся через `usePortfolioQuery` (фильтрация по
 * `stockId` локально). PnL уже рассчитан на backend. Buy/Sell переиспользуют
 * `BuyStockDialog`/`SellStockDialog` из features/trading — новая логика
 * торговли не реализуется.
 */
export default function StockDetailsView({ stock }: StockDetailsViewProps) {
  const { data: positions, isLoading: isPortfolioLoading } = usePortfolioQuery();
  const [tradeMode, setTradeMode] = useState<TradeMode | null>(null);
  const [logoFailed, setLogoFailed] = useState(false);

  const closeTrade = () => setTradeMode(null);
  const position = positions?.find((p) => p.stockId === stock.id) ?? null;

  const logoSrc = logoFailed
    ? resolveLogoUrl('/logos/default.svg')
    : resolveLogoUrl(stock.logoUrl);

  return (
    <Box>
      {/* Header */}
      <Paper sx={{ p: 3, mb: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, flexWrap: 'wrap' }}>
          <Avatar
            variant="rounded"
            src={logoSrc}
            alt={stock.companyName}
            onError={() => setLogoFailed(true)}
            sx={{ width: 64, height: 64, bgcolor: 'background.default', flexShrink: 0 }}
          />
          <Box sx={{ minWidth: 0 }}>
            <Typography variant="h4" component="h1" gutterBottom>
              {stock.companyName}
            </Typography>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap' }}>
              <Chip label={stock.ticker} color="primary" />
              <Chip label={stock.exchange} variant="outlined" />
              {stock.sector && (
                <Chip
                  icon={<Building2 size={15} />}
                  label={stock.sector}
                  variant="outlined"
                />
              )}
            </Box>
          </Box>
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

      {/* About / description */}
      {stock.description && (
        <Paper sx={{ p: 3, mt: 3 }}>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
            About
          </Typography>
          <Typography sx={{ whiteSpace: 'pre-line' }}>{stock.description}</Typography>
        </Paper>
      )}

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
          {stock.sector && <InfoRow label="Sector" value={stock.sector} />}
          {stock.website && (
            <Grid size={{ xs: 12, sm: 6 }}>
              <Typography variant="body2" color="text.secondary">
                Website
              </Typography>
              <Link
                href={stock.website}
                target="_blank"
                rel="noopener noreferrer"
                sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.5, fontWeight: 600 }}
              >
                <Globe size={15} />
                {stock.website}
              </Link>
            </Grid>
          )}
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
