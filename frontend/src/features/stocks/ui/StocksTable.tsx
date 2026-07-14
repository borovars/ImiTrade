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
  Avatar,
  Box,
} from '@mui/material';
import { Link, useNavigate } from 'react-router-dom';
import { Stock } from '../types/stockTypes';
import { formatMoney } from '@/shared/utils/format';
import BuyStockDialog from '@/features/trading/ui/BuyStockDialog';
import SellStockDialog from '@/features/trading/ui/SellStockDialog';
import StockSparkline from './StockSparkline';

type TradeMode = 'buy' | 'sell';

const ROWS_PER_PAGE_OPTIONS = [10, 20, 50];

/**
 * Базовый URL backend, от которого раздаются статические ресурсы (логотипы).
 * Совпадает с `baseURL` axios-клиента (`apiClient.ts`).
 */
const API_BASE_URL = (import.meta.env.VITE_API_URL as string | undefined) ?? '';

/** API-relative путь логотипа → абсолютный URL для `<img src>`. */
function resolveLogoUrl(logoUrl: string): string {
  return `${API_BASE_URL}${logoUrl}`;
}

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

/**
 * Таблица каталога акций.
 *
 * Колонки (слева направо): логотип + тикер, компания, биржа, цена, размер лота,
 * мини-график цены за год, действия (Купить/Продать). Логотип вписывается через
 * `objectFit: contain`, чтобы SVG не обрезался.
 *
 * Строки увеличены для читаемости: высота ячейки ~57px и увеличенный шрифт.
 * Диалоги Buy/Sell монтируются здесь же (локальное состояние tradeStock).
 *
 * Вся строка кликабельна — клик по любой ячейке открывает страницу акции
 * (`/stocks/{ticker}`). Кнопки Купить/Продать и ссылка-тикер прерывают
 * всплытие (`stopPropagation`), поэтому клик по ним не вызывает переход.
 */
export default function StocksTable({
  stocks,
  page,
  rowsPerPage,
  totalElements,
  loading = false,
  onPageChange,
  onRowsPerPageChange,
}: StocksTableProps) {
  const navigate = useNavigate();
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

  const goToStock = (ticker: string) => navigate(`/stocks/${ticker}`);

  return (
    <>
      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow sx={{ '& .MuiTableCell-head': { fontWeight: 700 } }}>
              <TableCell sx={{ pl: 2 }}>Тикер</TableCell>
              <TableCell>Компания</TableCell>
              <TableCell>Биржа</TableCell>
              <TableCell align="right">Цена</TableCell>
              <TableCell align="right">Размер лота</TableCell>
              <TableCell align="center">Цена за год</TableCell>
              <TableCell align="right">Действия</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {stocks.map((stock) => (
              <TableRow
                key={stock.id}
                hover
                onClick={() => goToStock(stock.ticker)}
                sx={{
                  height: 114,
                  cursor: 'pointer',
                  '& .MuiTableCell-root': { fontSize: '1rem' },
                }}
              >
                <TableCell sx={{ pl: 2 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                    <Avatar
                      variant="rounded"
                      src={resolveLogoUrl(stock.logoUrl)}
                      alt={stock.companyName}
                      sx={{
                        width: 36,
                        height: 36,
                        bgcolor: 'background.default',
                        flexShrink: 0,
                        '& .MuiAvatar-img': { objectFit: 'contain' },
                      }}
                    />
                    <Link
                      to={`/stocks/${stock.ticker}`}
                      onClick={(e) => e.stopPropagation()}
                      style={{ textDecoration: 'none', color: 'inherit', fontWeight: 600 }}
                    >
                      {stock.ticker}
                    </Link>
                  </Box>
                </TableCell>
                <TableCell>{stock.companyName}</TableCell>
                <TableCell>{stock.exchange}</TableCell>
                <TableCell align="right" sx={{ fontWeight: 600 }}>
                  {formatMoney(stock.currentPrice)}
                </TableCell>
                <TableCell align="right">{stock.lotSize}</TableCell>
                <TableCell align="center">
                  <StockSparkline ticker={stock.ticker} />
                </TableCell>
                <TableCell align="right">
                  <Button
                    size="small"
                    color="success"
                    onClick={(e) => {
                      e.stopPropagation();
                      openTrade(stock, 'buy');
                    }}
                    sx={{ mr: 1 }}
                  >
                    Купить
                  </Button>
                  <Button
                    size="small"
                    color="error"
                    onClick={(e) => {
                      e.stopPropagation();
                      openTrade(stock, 'sell');
                    }}
                  >
                    Продать
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
          labelRowsPerPage="Строк на странице:"
          labelDisplayedRows={({ from, to, count }) => `${from}–${to} из ${count}`}
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
