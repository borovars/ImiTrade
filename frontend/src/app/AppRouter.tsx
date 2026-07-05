import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import AppLayout from '@/widgets/layout/AppLayout';
import DashboardPage from '@/pages/DashboardPage';
import StocksPage from '@/pages/StocksPage';
import StockDetailPage from '@/pages/StockDetailPage';
import PortfolioPage from '@/pages/PortfolioPage';
import TransactionsPage from '@/pages/TransactionsPage';
import AccountPage from '@/pages/AccountPage';
import LoginPage from '@/pages/LoginPage';
import RegisterPage from '@/pages/RegisterPage';

export default function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route element={<AppLayout />}>
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/stocks" element={<StocksPage />} />
          <Route path="/stocks/:ticker" element={<StockDetailPage />} />
          <Route path="/portfolio" element={<PortfolioPage />} />
          <Route path="/transactions" element={<TransactionsPage />} />
          <Route path="/account" element={<AccountPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
