import PriceLineChart from '@/shared/components/charts/PriceLineChart';
import { getStockHistory } from '../api/stockHistoryApi';
import { queryKeys } from '@/shared/lib/queryKeys';

interface StockPriceChartProps {
  /** Тикер акции. Разные тикеры → независимые графики и кэш React Query. */
  ticker: string;
}

/**
 * Профессиональный интерактивный график цены акции (Area, только close).
 *
 * Тонкая обёртка над обобщённым {@link PriceLineChart}: подставляет тикер как
 * источник данных (`getStockHistory`), ключ кэша из реестра `queryKeys` и
 * включает lazy-load (колесо мыши + ЛКМ-панорама + фоновая предзагрузка старых
 * данных тем же интервалом). Вся визуальная часть, crosshair/tooltip/marker,
 * периоды и зум живут в {@link PriceLineChart} и переиспользуются графиком
 * стоимости портфеля.
 *
 * Модель взаимодействия (стиль Т-Инвестиций / MOEX) описана в
 * `shared/lib/chart/chartZoom.ts`. Данные — `GET /api/v1/stocks/{ticker}/history`
 * (backend сам идёт в MOEX ISS Candles и возвращает OHLCV; фронт рисует только
 * `close`, см. `stockHistoryApi`).
 */
export default function StockPriceChart({ ticker }: StockPriceChartProps) {
  return (
    <PriceLineChart
      seriesKey={ticker}
      fetchData={(period, from) => getStockHistory(ticker, from ?? new Date(), period)}
      queryKey={(period, fromIso) =>
        queryKeys.stockHistory(ticker, period, fromIso ?? fromDateToIso(new Date()))
      }
      lazyLoad
      tooltipValueLabel="Цена"
    />
  );
}

/** ISO-дата `yyyy-MM-dd` из Date (fallback, когда `fromIso` не передан). */
function fromDateToIso(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}
