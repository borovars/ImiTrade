import { useState } from 'react';
import { Box, Typography, Paper, Chip, Grid, Button, Skeleton, Link, Avatar } from '@mui/material';
import { TrendingUp, Globe, Building2 } from 'lucide-react';
import { Stock } from '@/features/stocks/types/stockTypes';
import { usePortfolioQuery } from '@/features/portfolio/model/usePortfolioQuery';
import { formatMoney, formatProfitLoss } from '@/shared/utils/format';
import BuyStockDialog from '@/features/trading/ui/BuyStockDialog';
import SellStockDialog from '@/features/trading/ui/SellStockDialog';
import StockPriceChart from './StockPriceChart';

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
 * Порядок блоков сверху вниз:
 * - Header (одна строка): логотип + название/тикер/биржа/сектор → цена →
 *   кнопки Купить (сверху) / Продать (снизу);
 * - User Position: позиция пользователя (если есть) с PnL;
 * - Price Chart: интерактивный график истории цены (StockPriceChart);
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
      {/* Header: одна строка — лого + название/чипы, цена, кнопки Купить/Продать. */}
      <Paper sx={{ p: 3, mb: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 3, flexWrap: 'wrap' }}>
          <Avatar
            variant="rounded"
            src={logoSrc}
            alt={stock.companyName}
            onError={() => setLogoFailed(true)}
            sx={{ width: 72, height: 72, bgcolor: 'background.default', flexShrink: 0, '& .MuiAvatar-img': { objectFit: 'contain' } }}
          />
          <Box sx={{ minWidth: 0, flexGrow: 1 }}>
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

          {/* Текущая цена. */}
          <Box sx={{ textAlign: { xs: 'left', sm: 'right' }, flexShrink: 0 }}>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 0.5 }}>
              Текущая цена
            </Typography>
            <Box sx={{ display: 'flex', alignItems: 'baseline', gap: 1, justifyContent: { xs: 'flex-start', sm: 'flex-end' } }}>
              <Box component={TrendingUp} sx={{ color: 'success.main', alignSelf: 'center' }} size={32} />
              <Typography variant="h4" component="span" sx={{ fontWeight: 700 }}>
                {formatMoney(stock.currentPrice)}
              </Typography>
            </Box>
          </Box>

          {/* Кнопки сделки стопкой: Купить сверху, Продать снизу. */}
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1, flexShrink: 0, minWidth: 150 }}>
            <Button
              variant="contained"
              color="success"
              fullWidth
              onClick={() => setTradeMode('buy')}
            >
              Купить
            </Button>
            <Button
              variant="contained"
              color="error"
              fullWidth
              onClick={() => setTradeMode('sell')}
            >
              Продать
            </Button>
          </Box>
        </Box>
      </Paper>

      {/* Price Chart: линейный график истории цены (lightweight-charts).
          key={ticker} гарантирует полный ремонт и сброс viewport'а при смене
          акции — графики разных тикеров полностью независимы. */}
      <Paper sx={{ p: 2, mb: 3 }}>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 1, px: 1 }}>
          История цены
        </Typography>
        <StockPriceChart key={stock.ticker} ticker={stock.ticker} />
      </Paper>

      {/* User Position */}
      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
          Ваша позиция
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
          <Typography color="text.secondary">Эта акция отсутствует в вашем портфеле.</Typography>
        )}
      </Paper>

      {/* About / description */}
      {stock.description && (
        <Paper sx={{ p: 3, mb: 3 }}>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
            О компании
          </Typography>
          <Typography sx={{ whiteSpace: 'pre-line' }}>{stock.description}</Typography>
        </Paper>
      )}

      {/* Company Information */}
      <Paper sx={{ p: 3 }}>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Информация о компании
        </Typography>
        <Grid container spacing={2}>
          <InfoRow label="Тикер" value={stock.ticker} />
          <InfoRow label="Компания" value={stock.companyName} />
          <InfoRow label="Биржа" value={stock.exchange} />
          <InfoRow label="Размер лота" value={`${stock.lotSize} акций`} />
          {stock.sector && <InfoRow label="Сектор" value={stock.sector} />}
          {stock.website && (
            <Grid size={{ xs: 12, sm: 6 }}>
              <Typography variant="body2" color="text.secondary">
                Сайт
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
  const lotsLabel = lotSize > 0 && quantity % lotSize === 0 ? ` (${quantity / lotSize} лот.)` : '';

  return (
    <Grid container spacing={2}>
      <InfoRow label="Количество" value={`${quantity}${lotsLabel}`} />
      <InfoRow label="Ср. цена" value={formatMoney(averagePrice)} />
      <InfoRow label="Стоимость позиции" value={formatMoney(positionValue)} />
      <Grid size={{ xs: 12 }}>
        <Typography variant="body2" color="text.secondary">
          Прибыль / Убыток
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
