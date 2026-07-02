export const API_ENDPOINTS = {
  GUEST: '/guest',
  AUTH: {
    LOGIN: '/auth/login',
    REGISTER: '/auth/register',
    REFRESH: '/auth/refresh',
  },
  ACCOUNT: {
    BASE: '/account',
  },
  STOCKS: {
    BASE: '/stocks',
  },
  PORTFOLIO: {
    BASE: '/portfolio',
  },
  TRANSACTIONS: {
    BASE: '/transactions',
  },
  TRADING: {
    BUY: '/trading/buy',
    SELL: '/trading/sell',
  },
} as const;
