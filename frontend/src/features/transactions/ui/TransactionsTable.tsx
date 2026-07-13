import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Typography,
} from '@mui/material';
import { Transaction } from '../types/transactionsTypes';
import { formatMoney, formatDateTime } from '@/shared/utils/format';

interface TransactionsTableProps {
  transactions: Transaction[];
}

/**
 * Таблица истории торговых операций.
 *
 * Чисто презентационный read-only компонент: данные берёт из props
 * (= page.content), ничего не мутирует. BUY подсвечивается зелёным
 * (success.main), SELL — красным (error.main) по теме MUI.
 */
export default function TransactionsTable({ transactions }: TransactionsTableProps) {
  return (
    <TableContainer component={Paper}>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Дата</TableCell>
            <TableCell>Тип</TableCell>
            <TableCell>Тикер</TableCell>
            <TableCell align="right">Количество</TableCell>
            <TableCell align="right">Цена</TableCell>
            <TableCell align="right">Сумма</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {transactions.map((tx) => (
            <TableRow key={tx.id} hover>
              <TableCell>{formatDateTime(tx.createdAt)}</TableCell>
              <TableCell>
                <Typography
                  component="span"
                  sx={{ color: tx.type === 'BUY' ? 'success.main' : 'error.main', fontWeight: 600 }}
                >
                  {tx.type}
                </Typography>
              </TableCell>
              <TableCell sx={{ fontWeight: 600 }}>{tx.ticker}</TableCell>
              <TableCell align="right">{tx.quantity}</TableCell>
              <TableCell align="right">{formatMoney(tx.price)}</TableCell>
              <TableCell align="right">{formatMoney(tx.totalAmount)}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
