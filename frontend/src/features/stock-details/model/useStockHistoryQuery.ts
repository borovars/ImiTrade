import { useQuery } from '@tanstack/react-query';
import { getStockHistory } from '../api/stockHistoryApi';
import { HistoryPeriodCode } from '@/shared/lib/chart/periods';
import type { HistoryPoint } from '@/shared/lib/chart/historyPoint';
import { queryKeys } from '@/shared/lib/queryKeys';

/**
 * Загрузка одного диапазона истории цены для графика.
 *
 * Каждый чанк ленивой подгрузки — отдельный вызов этого хука со своим
 * `from`/`period`. Ключ строится через общий реестр
 * (`queryKeys.stockHistory`), поэтому:
 *  - одинаковые диапазоны дедуплицируются React Query;
 *  - инвалидация префикса `stocks` захватит и историю.
 *
 * Опции `staleTime`/`refetchOnWindowFocus`/`retry` не дублируем — они заданы
 * как дефолты `QueryClient` (`queryProvider`).
 *
 * `fromIso` принимаем строкой (а не `Date`), чтобы ключ был стабильным:
 * два `new Date()` в одну миллисекунду дадут одинаковый ключ, но разные
 * объекты — строковое представление это снимает.
 */
export function useStockHistoryQuery(
  ticker: string,
  from: Date,
  period: HistoryPeriodCode,
  enabled = true
) {
  const fromIso = fromDateToIso(from);
  return useQuery<HistoryPoint[], Error>({
    queryKey: queryKeys.stockHistory(ticker, period, fromIso),
    queryFn: () => getStockHistory(ticker, from, period),
    enabled,
  });
}

function fromDateToIso(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}
