import { useQuery } from '@tanstack/react-query';
import { Box } from '@mui/material';
import { getStockSparkline } from '../api/stocksSparklineApi';
import { queryKeys } from '@/shared/lib/queryKeys';
import { SparklinePoint } from '../types/sparklineTypes';

interface StockSparklineProps {
  ticker: string;
}

/**
 * Крошечный SVG-спарклайн цены за последний год для строки каталога акций.
 *
 * Источник — `getStockSparkline` (`GET /api/v1/stocks/{ticker}/history`,
 * period=1M, from = сегодня − 365 дней). Рисуется чистым SVG без библиотек
 * графиков: 10 таких карт в таблице не должны тянуть 10 canvas'ов
 * lightweight-charts. Цвет линии — по тренду года (зелёный если вырос,
 * красный если упал), нейтральный серый при пустых данных.
 *
 * Линия скрытых/loading/ошибочных состояний не рисуется (ячейка остаётся
 * пустой) — sparkline декоративный, не должен мешать основной информации.
 */
const WIDTH = 88;
const HEIGHT = 32;
const PAD = 2;

// Левая граница диапазона: сегодня минус 365 дней. Модульная константа —
// стабильный ключ запроса на всё время жизни страницы.
function yearAgo(): Date {
  const d = new Date();
  d.setDate(d.getDate() - 365);
  return d;
}

export default function StockSparkline({ ticker }: StockSparklineProps) {
  const fromIso = fromDateToIso(yearAgo());
  const { data } = useQuery<SparklinePoint[], Error>({
    queryKey: queryKeys.stockHistory(ticker, '1M', fromIso),
    queryFn: () => getStockSparkline(ticker, yearAgo()),
    // Реиспользуем общий кэш истории: ключ совпадает с большим графиком
    // на странице акции для того же тикера/периода/from.
  });

  if (!data || data.length < 2) {
    return <Box sx={{ width: WIDTH, height: HEIGHT }} />;
  }

  const points = buildPolyline(data);
  const rose = data[data.length - 1].close >= data[0].close;
  const stroke = rose ? '#2e7d32' : '#c62828'; // success-like / error-like

  return (
    <Box sx={{ width: WIDTH, height: HEIGHT, display: 'inline-flex', verticalAlign: 'middle' }}>
      <svg width={WIDTH} height={HEIGHT} viewBox={`0 0 ${WIDTH} ${HEIGHT}`} aria-hidden="true">
        <polyline
          points={points}
          fill="none"
          stroke={stroke}
          strokeWidth={1.5}
          strokeLinejoin="round"
          strokeLinecap="round"
        />
      </svg>
    </Box>
  );
}

/** Превращает точки свечей в строку координат `x,y x,y …`. */
function buildPolyline(data: SparklinePoint[]): string {
  const closes = data.map((d) => d.close);
  const min = Math.min(...closes);
  const max = Math.max(...closes);
  const span = max - min || 1;
  const innerW = WIDTH - PAD * 2;
  const innerH = HEIGHT - PAD * 2;
  return data
    .map((d, i) => {
      const x = PAD + (i / (data.length - 1)) * innerW;
      const y = PAD + innerH - ((d.close - min) / span) * innerH;
      return `${x.toFixed(1)},${y.toFixed(1)}`;
    })
    .join(' ');
}

function fromDateToIso(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}
