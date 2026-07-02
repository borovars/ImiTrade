import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
} from '@mui/material';
import { Stock } from '../types/stockTypes';

interface StocksTableProps {
  stocks: Stock[];
}

export default function StocksTable({ stocks }: StocksTableProps) {
  return (
    <TableContainer component={Paper}>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Ticker</TableCell>
            <TableCell>Company Name</TableCell>
            <TableCell>Exchange</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {stocks.map((stock) => (
            <TableRow key={stock.id} hover>
              <TableCell sx={{ fontWeight: 600 }}>{stock.ticker}</TableCell>
              <TableCell>{stock.companyName}</TableCell>
              <TableCell>{stock.exchange}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
