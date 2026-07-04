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
  portfolio: ['portfolio'] as const,
  transactions: ['transactions'] as const,
};
