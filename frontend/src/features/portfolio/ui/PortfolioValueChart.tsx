import PriceLineChart from '@/shared/components/charts/PriceLineChart';
import { getPortfolioHistory } from '../api/portfolioHistoryApi';
import { queryKeys } from '@/shared/lib/queryKeys';

/**
 * График изменения стоимости портфеля во времени (Area, только value).
 *
 * Тонкая обёртка над обобщённым {@link PriceLineChart}: подставляет источник
 * данных (`getPortfolioHistory`) и ключ кэша из реестра `queryKeys`. Вся
 * визуальная часть, crosshair/tooltip/marker и переключатель периодов живут в
 * {@link PriceLineChart} и переиспользуются графиком цены акции.
 *
 * В отличие от графика цены акции, здесь `lazyLoad=false`: backend отдаёт уже
 * готовый временной ряд на каждый период, догружать «в прошлое» по кусочкам не
 * нужно. Источник — `GET /api/v1/portfolio/history` (backend сам реконструирует
 * историю, replay'я транзакции против исторических цен MOEX).
 */
export default function PortfolioValueChart() {
  return (
    <PriceLineChart
      seriesKey="portfolio"
      fetchData={(period) => getPortfolioHistory(period)}
      queryKey={(period) => queryKeys.portfolioHistory(period)}
      lazyLoad={false}
      tooltipValueLabel="Стоимость"
    />
  );
}
