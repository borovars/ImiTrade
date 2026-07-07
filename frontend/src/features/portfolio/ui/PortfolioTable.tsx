import { useState } from 'react';
import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Button,
  Typography,
} from '@mui/material';
import { PortfolioPosition } from '../types/portfolioTypes';
import { Stock } from '@/features/stocks/types/stockTypes';
import { formatMoney, formatProfitLoss } from '@/shared/utils/format';
import SellStockDialog from '@/features/trading/ui/SellStockDialog';

interface PortfolioTableProps {
  positions: PortfolioPosition[];
}

/**
 * Таблица позиций портфеля.
 *
 * Финансовые показатели берёт из backend (pnl уже рассчитан). Position Value
 * — единственное производное значение: quantity × currentPrice (display-умножение
 * из примитивов backend, а не пересчёт PnL/агрегатов).
 *
 * Кнопка «Sell» в строке открывает уже существующий SellStockDialog (новая
 * логика продажи не реализуется). После успешной продажи trading-мутация
 * инвалидирует queryKeys.portfolio, и таблица обновляется автоматически.
 */
export default function PortfolioTable({ positions }: PortfolioTableProps) {
  const [sellPosition, setSellPosition] = useState<PortfolioPosition | null>(null);

  const closeSell = () => setSellPosition(null);

  // SellStockDialog ожидает объект формы Stock (id/exchange/lotSize). Позиция
  // портфеля использует stockId и не содержит exchange — собираем совместимый
  // объект, прокидывая lotSize для корректного множителя в форме продажи.
  const toStock = (position: PortfolioPosition): Stock => ({
    id: position.stockId,
    ticker: position.ticker,
    companyName: position.companyName,
    exchange: '',
    currentPrice: position.currentPrice,
    lotSize: position.lotSize,
    // Диалог продажи не отображает логотип; подставляем default-плейсхолдер,
    // т.к. позиция портфеля не несёт полный набор полей Stock.
    logoUrl: '/logos/default.svg',
  });

  return (
    <>
      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Ticker</TableCell>
              <TableCell>Company</TableCell>
              <TableCell align="right">Quantity</TableCell>
              <TableCell align="right">Average Price</TableCell>
              <TableCell align="right">Current Price</TableCell>
              <TableCell align="right">Position Value</TableCell>
              <TableCell align="right">Profit / Loss</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {positions.map((position) => {
              const profitLoss = formatProfitLoss(position.pnl);
              const positionValue = position.quantity * position.currentPrice;
              const lotsLabel =
                position.lotSize > 0 && position.quantity % position.lotSize === 0
                  ? ` (${position.quantity / position.lotSize} lots)`
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
                  <TableCell align="right">{formatMoney(positionValue)}</TableCell>
                  <TableCell align="right">
                    <Typography component="span" sx={{ color: profitLoss.color, fontWeight: 600 }}>
                      {profitLoss.text}
                    </Typography>
                  </TableCell>
                  <TableCell align="right">
                    <Button size="small" color="error" onClick={() => setSellPosition(position)}>
                      Sell
                    </Button>
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </TableContainer>

      {sellPosition && (
        <SellStockDialog stock={toStock(sellPosition)} open onClose={closeSell} />
      )}
    </>
  );
}
