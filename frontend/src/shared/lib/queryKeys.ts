/**
 * Единый реестр query-ключей.
 *
 * Канал взаимодействия между фичами: любая мутация, которой нужно
 * инвалидировать чужие запросы (например, trading → account/stocks/portfolio),
 * импортирует ключи отсюда, а не из внутренней модели другой фичи.
 *
 * Каждое значение — кортеж `as const`, чтобы React Query выводилliteral тип ключа.
 */
export const queryKeys = {
  account: ['account'] as const,
  stocks: ['stocks'] as const,
  /** Ключ конкретной акции по тикеру. Вкладывается в namespace `stocks`,
   *  поэтому инвалидация `queryKeys.stocks` (prefix) захватит и detail-запрос. */
  stockDetails: (ticker: string) => ['stocks', 'detail', ticker] as const,
  /** История цены акции для графика. Период и левая граница (`fromIso`) —
   *  часть ключа: разные диапазоны кэшируются независимо, а инвалидация
   *  префикса `stocks` захватит и их. */
  stockHistory: (ticker: string, period: string, fromIso: string) =>
    ['stocks', 'history', ticker, period, fromIso] as const,
  portfolio: ['portfolio'] as const,
  transactions: ['transactions'] as const,
};
