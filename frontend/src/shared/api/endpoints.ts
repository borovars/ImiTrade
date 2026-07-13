export const API_ENDPOINTS = {
  GUEST: '/api/v1/guest',
  AUTH: {
    LOGIN: '/api/v1/auth/login',
    REGISTER: '/api/v1/auth/register',
    REFRESH: '/api/v1/auth/refresh',
  },
  USERS: {
    ME: '/api/v1/users/me',
  },
  ACCOUNT: {
    BASE: '/api/v1/account',
  },
  STOCKS: {
    BASE: '/api/v1/stocks',
    /** История цены акции: GET /api/v1/stocks/{ticker}/history?period=&from=. */
    history: (ticker: string) => `/api/v1/stocks/${ticker}/history`,
  },
  PORTFOLIO: {
    BASE: '/api/v1/portfolio',
  },
  TRANSACTIONS: {
    BASE: '/api/v1/transactions',
  },
  TRADING: {
    BUY: '/api/v1/trades/buy',
    SELL: '/api/v1/trades/sell',
  },
} as const;
