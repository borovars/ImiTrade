import { LayoutDashboard, TrendingUp, Wallet, Receipt } from 'lucide-react';
import type { ReactNode } from 'react';

/**
 * Пункты основной навигации приложения.
 *
 * Единый источник истины для desktop-навигации (`DesktopNav`) и мобильного
 * drawer'а (`MobileNav`) — чтобы наборы пунктов не расходились между видами.
 * About сюда НЕ входит — он живёт отдельной иконкой у бренда в `AppTopBar`.
 */
export interface NavItem {
  label: string;
  path: string;
  icon: ReactNode;
  /** `end` нужен для `/dashboard`, чтобы он не ловил все маршруты (prefix-match). */
  end?: boolean;
}

export const NAV_ITEMS: NavItem[] = [
  { label: 'Главная', path: '/dashboard', icon: <LayoutDashboard size={20} />, end: true },
  { label: 'Акции', path: '/stocks', icon: <TrendingUp size={20} /> },
  { label: 'Портфель', path: '/portfolio', icon: <Wallet size={20} /> },
  { label: 'Операции', path: '/transactions', icon: <Receipt size={20} /> },
];
