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
  TablePagination,
} from '@mui/material';
import { Link } from 'react-router-dom';
import { Stock } from '../types/stockTypes';
import { formatMoney } from '@/shared/utils/format';
import BuyStockDialog from '@/features/trading/ui/BuyStockDialog';
import SellStockDialog from '@/features/trading/ui/SellStockDialog';

type TradeMode = 'buy' | 'sell';

const ROWS_PER_PAGE_OPTIONS = [10, 20, 50];

interface StocksTableProps {
  stocks: Stock[];
  /** 0-based индекс страницы (контракт Spring Page). */
  page: number;
  rowsPerPage: number;
  totalElements: number;
  /** Блокировка контролов на время подгрузки новой страницы (placeholder data). */
  loading?: boolean;
  onPageChange: (page: number) => void;
  onRowsPerPageChange: (size: number) => void;
}

export default function StocksTable({
  stocks,
  page,
  rowsPerPage,
  totalElements,
  loading = false,
  onPageChange,
  onRowsPerPageChange,
}: StocksTableProps) {
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
              <TableCell align="right">Lot Size</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {stocks.map((stock) => (
              <TableRow key={stock.id} hover>
                <TableCell sx={{ fontWeight: 600 }}>
                  <Link
                    to={`/stocks/${stock.ticker}`}
                    style={{ textDecoration: 'none', color: 'inherit' }}
                  >
                    {stock.ticker}
                  </Link>
                </TableCell>
                <TableCell>{stock.companyName}</TableCell>
                <TableCell>{stock.exchange}</TableCell>
                <TableCell align="right">{formatMoney(stock.currentPrice)}</TableCell>
                <TableCell align="right">{stock.lotSize}</TableCell>
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
        <TablePagination
          component="div"
          count={totalElements}
          page={page}
          rowsPerPage={rowsPerPage}
          rowsPerPageOptions={ROWS_PER_PAGE_OPTIONS}
          disabled={loading}
          onPageChange={(_, newPage) => onPageChange(newPage)}
          onRowsPerPageChange={(e) => onRowsPerPageChange(Number(e.target.value))}
        />
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
