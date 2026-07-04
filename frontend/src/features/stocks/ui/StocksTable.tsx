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
} from '@mui/material';
import { Stock } from '../types/stockTypes';
import { formatMoney } from '@/shared/utils/format';
import BuyStockDialog from '@/features/trading/ui/BuyStockDialog';
import SellStockDialog from '@/features/trading/ui/SellStockDialog';

type TradeMode = 'buy' | 'sell';

interface StocksTableProps {
  stocks: Stock[];
}

export default function StocksTable({ stocks }: StocksTableProps) {
  const [tradeStock, setTradeStock] = useState<Stock | null>(null);
  const [tradeMode, setTradeMode] = useState<TradeMode | null>(null);

  const openTrade = (stock: Stock, mode: TradeMode) => {
    setTradeStock(stock);
    setTradeMode(mode);
  };

  const closeTrade = () => {
    setTradeStock(null);
    setTradeMode(null);
  };

  return (
    <>
      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Ticker</TableCell>
              <TableCell>Company Name</TableCell>
              <TableCell>Exchange</TableCell>
              <TableCell align="right">Price</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {stocks.map((stock) => (
              <TableRow key={stock.id} hover>
                <TableCell sx={{ fontWeight: 600 }}>{stock.ticker}</TableCell>
                <TableCell>{stock.companyName}</TableCell>
                <TableCell>{stock.exchange}</TableCell>
                <TableCell align="right">{formatMoney(stock.currentPrice)}</TableCell>
                <TableCell align="right">
                  <Button
                    size="small"
                    color="success"
                    onClick={() => openTrade(stock, 'buy')}
                    sx={{ mr: 1 }}
                  >
                    Buy
                  </Button>
                  <Button size="small" color="error" onClick={() => openTrade(stock, 'sell')}>
                    Sell
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      {tradeStock && tradeMode === 'buy' && (
        <BuyStockDialog stock={tradeStock} open onClose={closeTrade} />
      )}
      {tradeStock && tradeMode === 'sell' && (
        <SellStockDialog stock={tradeStock} open onClose={closeTrade} />
      )}
    </>
  );
}
