import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Box, Button, CircularProgress, Typography } from '@mui/material';
import {
  AreaSeries,
  type IChartApi,
  type IPriceLine,
  type ISeriesApi,
  type Time,
  type UTCTimestamp,
  createChart,
} from 'lightweight-charts';
import { useQuery } from '@tanstack/react-query';
import { StateError, TableSkeleton } from '@/shared/components';
import { formatDateTime, formatMoney } from '@/shared/utils/format';
import { HistoryPeriodCode } from '@/shared/lib/chart/periods';
import type { HistoryPoint } from '@/shared/lib/chart/historyPoint';
import { AXIS_TEXT_COLOR, chartBaseOptions, areaSeriesOptions } from '@/shared/lib/chart/chartTheme';
import {
  type ViewportState,
  BACKGROUND_PRELOAD_CHUNKS,
  BACKGROUND_PRELOAD_DELAY_MS,
  CANONICAL_PERIODS,
  PERIOD_LOOKBACK_DAYS,
  addDays,
  createInitialViewport,
  fromTimestamp,
  needsLazyLoad,
  nextLazyLoadFrom,
  throttle,
} from '@/shared/lib/chart/chartZoom';

export interface PriceLineChartProps {
  /**
   * Ключ пере монтирования графика. Разные значения → независимые графики и
   * кэш React Query. Для цены акции это тикер, для портфеля — константа
   * (напр. `'portfolio'`).
   */
  seriesKey: string;
  /**
   * Загрузчик данных: возвращает точки `{ time, value }` для выбранного периода
   * (и, при lazy-load, левой границы `from`). Реализация скрыта в фиче —
   * график не знает, откуда берутся данные (MOEX candles / portfolio history).
   */
  fetchData: (period: HistoryPeriodCode, from?: Date) => Promise<HistoryPoint[]>;
  /**
   * Фабрика ключа React Query. График не строит ключ сам — фича передаёт свой
   * ключ из общего реестра `queryKeys`, чтобы инвалидация работала корректно.
   */
  queryKey: (period: HistoryPeriodCode, fromIso?: string) => readonly unknown[];
  /**
   * Включить lazy-load (колесо мыши + ЛКМ-панорама + фоновая предзагрузка
   * старых данных тем же интервалом). По умолчанию `true` (график цены акции).
   * График стоимости портфеля получает готовый ряд и ставит `false`.
   */
  lazyLoad?: boolean;
  /** Подпись значения в tooltip и оси Y (напр. «Цена» / «Стоимость»). */
  tooltipValueLabel?: string;
  /** Начальный период (кнопка). По умолчанию `1D`. */
  initialPeriod?: HistoryPeriodCode;
}

/** Точка для lightweight-charts: время в UNIX-секундах + значение. */
interface ChartPoint {
  time: UTCTimestamp;
  value: number;
}

const CHART_HEIGHT_PX = 380;
/**
 * Минимальный зазор (px) между подписью текущего значения и min/max на оси Y.
 * При сближении меток подпись current скрывается — это убирает дребезг/смену
 * меток местами, когда текущее значение почти равно min или max.
 */
const MIN_LABEL_GAP_PX = 22;

/**
 * Профессиональный интерактивный линейный график (Area, только value).
 *
 * Переиспользуется графиком цены акции (`features/stock-details`) и графиком
 * стоимости портфеля (`features/portfolio`). Источник данных и ключ кэша
 * передаются снаружи (`fetchData` / `queryKey`), поэтому компонент не привязан
 * ни к конкретному эндпоинту, ни к конкретной фиче.
 *
 * Модель взаимодействия (стиль Т-Инвестиций / MOEX), только при `lazyLoad=true`:
 *  - кнопки 1D / 1W / 1M / 1Y — выбор интервала свечи и стартовой глубины
 *    (1D=день/3мес, 1W=неделя/5мес, 1M=месяц/3года, 1Y=квартал/10лет);
 *  - перетаскивание ЛКМ — панорамирование; выход за левый край → lazy-подгрузка
 *    того же интервала (бесконечный скролл в прошлое);
 *  - колесо мыши — догрузка старых данных порциями (1D→+неделя, 1W→+2 месяца,
 *    1M→+год, 1Y→+2 года за щелчок). Не масштабирует ширину свечей, а
 *    расширяет видимый диапазон влево — как в брокерских приложениях.
 *
 * При `lazyLoad=false` (портфель) — только кнопки периода; ряд рисуется целиком
 * по одному запросу на период, догрузки нет.
 *
 * Визуально: линия `#3dba8d` + прозрачный градиент, чистый фон без сетки,
 * правый край прибит к последней точке, resize через `autoSize`. Crosshair
 * реализован собственным DOM-overlay: чёрная пунктирная вертикальная
 * направляющая + фирменный маркер + tooltip (дата + значение).
 */
export default function PriceLineChart({
  seriesKey,
  fetchData,
  queryKey,
  lazyLoad = true,
  tooltipValueLabel = 'Цена',
  initialPeriod = '1D',
}: PriceLineChartProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const chartRef = useRef<IChartApi | null>(null);
  const seriesRef = useRef<ISeriesApi<'Area'> | null>(null);
  // Плавающее окно tooltip (дата + значение под курсором). Создаётся один раз и
  // позиционируется/скрывается через прямую запись в DOM — движение мыши не
  // должно перерисовывать React (см. readme: «пишется в DOM напрямую, без re-render»).
  const tooltipRef = useRef<HTMLDivElement | null>(null);
  // Вертикальная пунктирная линия-направляющая под курсором (чёрная, во всю
  // высоту холста) и круглый маркер в точке пересечения линии графика с этой
  // направляющей. Живут в DOM, управляются из того же обработчика `onMove`, что
  // и tooltip — без re-render React. `pointerEvents: 'none'` — не мешают графику.
  const vLineRef = useRef<HTMLDivElement | null>(null);
  const markerRef = useRef<HTMLDivElement | null>(null);
  // Price lines для min/max видимого диапазона (третья метка — текущее значение —
  // рисуется самой серией через `priceLineVisible`). Пересоздаются при смене
  // видимого диапазона, чтобы на оси значения всегда было ровно 3 значения.
  const priceLinesRef = useRef<IPriceLine[]>([]);

  // Viewport и кэш данных — в refs: движение мыши не должно перерисовывать React.
  const lazyEnabledRef = useRef(lazyLoad);
  lazyEnabledRef.current = lazyLoad;
  const viewportRef = useRef<ViewportState>(createInitialViewport(initialPeriod));
  const mergedRef = useRef<Map<number, number>>(new Map());
  const loadingRef = useRef(false);
  const seriesKeyRef = useRef(seriesKey);
  seriesKeyRef.current = seriesKey;
  // Фиксируем fetchData/queryKey на момент запроса, чтобы замыкания внутри
  // эффектов и lazy-загрузчика видели актуальный загрузчик (он стабилен у
  // обёрток-фич через useCallback/useMemo на серии).
  const fetchDataRef = useRef(fetchData);
  fetchDataRef.current = fetchData;

  // React-state — для перерисовки UI вокруг графика.
  const [period, setPeriod] = useState<HistoryPeriodCode>(initialPeriod);
  const [isFetchingMore, setIsFetchingMore] = useState(false);

  // ───────────────────────── Запросы данных ─────────────────────────
  // Главный запрос: активный период, стартовая глубина = `сейчас − lookback`.
  // Правую границу бэкенд подставляет сам (`till = LocalDate.now()`).
  // `from` имеет смысл только при lazyLoad; иначе весь диапазон задаёт период.
  const initialFrom = useMemo(() => addDays(new Date(), -PERIOD_LOOKBACK_DAYS[period]), [period]);
  const fromIso = useMemo(() => fromDateToIso(initialFrom), [initialFrom]);
  const queryKeyArgs = useMemo(
    () => (lazyLoad ? (queryKey(period, fromIso) as readonly unknown[]) : (queryKey(period) as readonly unknown[])),
    [queryKey, period, fromIso, lazyLoad]
  );
  const mainQuery = useQuery<HistoryPoint[], Error>({
    queryKey: queryKeyArgs,
    queryFn: () => (lazyLoad ? fetchData(period, initialFrom) : fetchData(period)),
  });

  // ───────────────────────── Создание графика ─────────────────────────
  // Создаётся один раз на seriesKey. Контейнер рендерится всегда, поэтому эффект
  // находит его сразу.
  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    const chart = createChart(container, {
      ...chartBaseOptions,
      width: container.clientWidth,
      height: CHART_HEIGHT_PX,
      autoSize: true,
    });
    const series = chart.addSeries(AreaSeries, areaSeriesOptions);
    const merged = mergedRef.current; // фиксируем на время эффекта для cleanup
    chartRef.current = chart;
    seriesRef.current = series;

    // Плавающее окно tooltip: дата + значение под курсором. Узел живёт внутри
    // контейнера графика и управляется напрямую через DOM (без re-render).
    ensureOverlayStyle(container.ownerDocument);
    const tooltip = container.ownerDocument.createElement('div');
    tooltip.setAttribute('data-chart-tooltip', '');
    Object.assign(tooltip.style, tooltipStyle as CSSStyleDeclaration);
    container.appendChild(tooltip);
    tooltipRef.current = tooltip;

    // Вертикальная пунктирная направляющая (чёрная) + круглый маркер на линии
    // графика. Добавляются ПЕРЕД tooltip, чтобы tooltip лежал поверх них.
    const vLine = container.ownerDocument.createElement('div');
    vLine.setAttribute('data-chart-crosshair-vline', '');
    Object.assign(vLine.style, vLineStyle as CSSStyleDeclaration);
    container.appendChild(vLine);
    vLineRef.current = vLine;

    const marker = container.ownerDocument.createElement('div');
    marker.setAttribute('data-chart-crosshair-marker', '');
    marker.className = 'cct-marker';
    Object.assign(marker.style, markerStyle as CSSStyleDeclaration);
    container.appendChild(marker);
    markerRef.current = marker;

    // Подписка на НАТИВНОЕ движение мыши, а не на crosshair-события библиотеки:
    // crosshair срабатывает только когда под курсором есть точка, и между точками
    // окно «замирало». Нативный mousemove стреляет на каждый пиксель, поэтому окно
    // постоянно следует за курсором. По X-координате через `coordinateToTime`
    // получаем ближайшую точку данных и её значение из кэша. Из этого же обработчика
    // позиционируем вертикальную направляющую и маркер — единый путь, без
    // лишних источников правды и без re-render React.
    const hideOverlays = () => {
      tooltip.style.display = 'none';
      vLine.style.display = 'none';
      marker.classList.remove('cct-marker-visible');
    };
    // Перерисовка overlay (vLine + marker + tooltip) по X-координате курсора
    // внутри холста. Единый путь для `mousemove` и для перепозиционирования при
    // зуме колёсиком (когда мышь неподвижна, но под курсором уже другая точка).
    // `cursorY` нужен только для позиционирования tooltip; vLine и marker
    // снапятся к точке данных, а не к самому курсору.
    const repositionOverlays = (x: number, cursorY: number) => {
      // Ближайшая точка данных к X курсора.
      const time = chart.timeScale().coordinateToTime(x);
      if (time === null) {
        hideOverlays();
        return;
      }
      const value = merged.get(time as number);
      if (value === undefined) {
        hideOverlays();
        return;
      }
      // X-координата самой точки данных (снап), чтобы маркер точно лежал на
      // линии графика, а не «плавал» между точками за курсором.
      const pointX = chart.timeScale().timeToCoordinate(time as Time);
      // Y-координата значения этой точки на холсте.
      const pointY = series.priceToCoordinate(value);

      // Вертикальная направляющая — по X точки данных, во всю высоту холста.
      if (pointX !== null) {
        vLine.style.left = `${pointX}px`;
        vLine.style.display = 'block';
      } else {
        vLine.style.display = 'none';
      }
      // Маркер — в точке пересечения направляющей и линии графика.
      if (pointX !== null && pointY !== null) {
        marker.style.left = `${pointX}px`;
        marker.style.top = `${pointY}px`;
        marker.classList.add('cct-marker-visible');
      } else {
        marker.classList.remove('cct-marker-visible');
      }

      tooltip.innerHTML =
        `<div><span class="cct-label">Дата:</span> ${formatDateTime(new Date((time as number) * 1000))}</div>` +
        `<div><span class="cct-label">${tooltipValueLabel}:</span> ${formatMoney(value)}</div>`;
      positionTooltip(tooltip, x, cursorY);
      tooltip.style.display = 'block';
    };
    // Последняя позиция курсора внутри холста. При зуме колёсиком мышь
    // неподвижна, `mousemove` не стреляет — берём X отсюда, чтобы overlay
    // перепозиционировался на новую точку данных под курсором.
    let lastCursorX: number | null = null;
    const onMove = (e: MouseEvent) => {
      const rect = container.getBoundingClientRect();
      const x = e.clientX - rect.left;
      const y = e.clientY - rect.top;
      // За пределами холста (например, над осью) — скрываем всё и забываем
      // позицию, чтобы зум «вхолостую» не показывал зависший overlay.
      if (x < 0 || y < 0 || x > rect.width || y > rect.height) {
        lastCursorX = null;
        hideOverlays();
        return;
      }
      lastCursorX = x;
      repositionOverlays(x, y);
    };
    const onLeave = () => {
      lastCursorX = null;
      hideOverlays();
    };
    container.addEventListener('mousemove', onMove);
    container.addEventListener('mouseleave', onLeave);

    // Пересчёт min/max/current price lines + проверка lazy-подгрузки при любом
    // движении видимого диапазона (панорамирование, зум, смена данных). Пока
    // курсор над графиком, также перепозиционируем overlay: при зуме колёсиком
    // `mousemove` не стреляет (мышь неподвижна), но под курсором уже другая
    // точка данных — без этого overlay завис бы на старой позиции.
    //
    // Throttle (а не debounce): при колёсном зуме событие range-change стреляет
    // непрерывно, и debounce каждый раз сбрасывал таймер, ожидая тишины — запрос
    // задерживался, пока пользователь не прекратит скролл. Leading-edge throttle
    // уходит на первом импульсе и группирует повторы не чаще раза в 220мс.
    const throttledLazy = throttle(() => maybeTriggerLazyLoad(), 220);
    const onRangeChange = () => {
      updatePriceLines();
      if (lastCursorX !== null) repositionOverlays(lastCursorX, lastCursorX);
      if (lazyEnabledRef.current) throttledLazy.call();
    };
    chart.timeScale().subscribeVisibleLogicalRangeChange(onRangeChange);

    return () => {
      container.removeEventListener('mousemove', onMove);
      container.removeEventListener('mouseleave', onLeave);
      chart.timeScale().unsubscribeVisibleLogicalRangeChange(onRangeChange);
      throttledLazy.cancel();
      tooltip.remove();
      vLine.remove();
      marker.remove();
      tooltipRef.current = null;
      vLineRef.current = null;
      markerRef.current = null;
      chart.remove();
      chartRef.current = null;
      seriesRef.current = null;
      priceLinesRef.current = [];
      // Сброс под следующий seriesKey.
      viewportRef.current = createInitialViewport(initialPeriod);
      merged.clear();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [seriesKey]);

  // ───────────────────── Применение данных активного периода ─────────────
  // Перерисовка при первом получении или при смене периода: полностью заменяем
  // кэш и сбрасываем viewport под новый диапазон. Состояние loading/error/empty
  // не ломает график — контейнер всегда в DOM.
  useEffect(() => {
    const series = seriesRef.current;
    if (!series || !mainQuery.data) return;

    viewportRef.current = createInitialViewport(period);

    mergedRef.current.clear();
    for (const p of mainQuery.data) mergedRef.current.set(toTimestamp(p.time), p.value);
    flushSeries();

    // Подгон видимого диапазона под стартовую глубину (правый край = сейчас) и
    // первичная отрисовка min/max price lines.
    requestAnimationFrame(() => {
      const chart = chartRef.current;
      if (chart) fitInitialVisibleRange(chart, viewportRef.current);
      updatePriceLines();
    });

    if (!lazyEnabledRef.current) return;

    // ─── Фоновая предзагрузка: с задержкой тянем чанк в прошлое, чтобы к
    // моменту прокрутки колесом/драгом данные уже были в кэше. Молча (без
    // индикатора), последовательно (по `loadingRef`), стоп при достижении
    // границы истории. Задержка нужна, чтобы не конкурировать с основным
    // запросом и не перегружать MOEX (он лимитирует соединения с одного IP —
    // агрессивная предзагрузка приводила к таймаутам при открытии нескольких
    // акций). Главная причина «медленного скролла влево» — запрос стартовал
    // только после того, как пользователь уже уехал за край данных.
    let cancelled = false;
    const timer = setTimeout(() => {
      (async () => {
        for (let i = 0; i < BACKGROUND_PRELOAD_CHUNKS; i++) {
          if (cancelled) return;
          const before = viewportRef.current.loadedFrom.getTime();
          await requestLazyLoad(nextLazyLoadFrom(viewportRef.current), true);
          // Если `loadedFrom` не сдвинулся — достигнут предел истории или запрос
          // не прошёл; дальше предзагружать нечего.
          if (viewportRef.current.loadedFrom.getTime() === before) return;
          if (viewportRef.current.oldestAvailableDate !== null) return;
        }
      })();
    }, BACKGROUND_PRELOAD_DELAY_MS);
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mainQuery.data, period]);

  // ───────────────────────── Выбор периода ─────────────────────────
  const handleSelectPeriod = useCallback((next: HistoryPeriodCode) => {
    setPeriod(next);
    viewportRef.current = { ...viewportRef.current, period: next };
  }, []);

  // ───────────────────────── Внутренние хелперы (стабильные) ─────────────────────────

  /**
   * Пересчитывает ценовые линии оси Y: min (range.from), max (range.to) и
   * текущее значение последней точки. Все три рисуются единым путём —
   * `createPriceLine` с `lineVisible: false` (на холсте линий нет, видны только
   * подписи-«таблетки» на оси). Сама линия текущего значения у серии отключена в
   * теме (`priceLineVisible: false`) — раньше именно конфликт нативной линии
   * серии и кастомных min/max давал «дребезг» и смену меток местами при
   * current≈min/max.
   *
   * Приоритет current («на переднем плане»): рисуется ВСЕГДА. min/max рисуются по
   * реальным позициям, но если подпись min или max подходит к current ближе
   * `MIN_LABEL_GAP_PX`, эту крайнюю метку пропускаем — current перекрывает её.
   *
   * Определён раньше `mergeData` (который его вызывает), чтобы избежать
   * использования до объявления (TDZ).
   */
  const updatePriceLines = useCallback(() => {
    const series = seriesRef.current;
    if (!series) return;
    // getVisibleRange() возвращает {from=min, to=max} текущего ценового масштаба.
    const range = series.priceScale().getVisibleRange();
    // Бывает null на старте/при пустых данных.
    if (!range) {
      for (const pl of priceLinesRef.current) series.removePriceLine(pl);
      priceLinesRef.current = [];
      return;
    }
    // Удаляем старые линии перед добавлением новых (значения изменились).
    for (const pl of priceLinesRef.current) series.removePriceLine(pl);

    // Текущее значение = значение последней точки (самая свежая точка кэша).
    const merged = mergedRef.current;
    let currentValue: number | null = null;
    if (merged.size > 0) {
      let latestTs = 0;
      for (const ts of merged.keys()) if (ts > latestTs) latestTs = ts;
      currentValue = merged.get(latestTs) ?? null;
    }

    // canvas-рендерер lightweight-charts не понимает `transparent` и рисует фон
    // подписи чёрным, если цвет не задан. Поэтому `axisLabelColor = '#ffffff'`:
    // подпись рисуется как белый «пилл» (фон совпадает с MUI Paper), поверх —
    // чёрный текст. Без `title` — на оси только сами значения min/max/current.
    const opts = {
      color: '#ffffff',
      lineVisible: false,
      axisLabelVisible: true,
      axisLabelColor: '#ffffff',
      axisLabelTextColor: AXIS_TEXT_COLOR,
    };

    // Координаты для проверки коллизий. Текущее значение «на переднем плане»: его
    // метка рисуется всегда, а min/max пропускаются, если налезают на current.
    const coordMin = series.priceToCoordinate(range.from);
    const coordMax = series.priceToCoordinate(range.to);
    const coordCur = currentValue !== null ? series.priceToCoordinate(currentValue) : null;
    const tooClose = (a: number | null, b: number | null) => a !== null && b !== null && Math.abs(a - b) < MIN_LABEL_GAP_PX;

    // min/max — на «заднем плане»: рисуются, только если не перекрыты current.
    const lines: ReturnType<typeof series.createPriceLine>[] = [];
    if (coordCur === null || coordMin === null || !tooClose(coordMin, coordCur)) {
      lines.push(series.createPriceLine({ ...opts, price: range.from }));
    }
    if (coordCur === null || coordMax === null || !tooClose(coordMax, coordCur)) {
      lines.push(series.createPriceLine({ ...opts, price: range.to }));
    }
    // Текущее значение — всегда (добавляется последней → поверх min/max).
    if (currentValue !== null && coordCur !== null) {
      lines.push(series.createPriceLine({ ...opts, price: currentValue }));
    }
    priceLinesRef.current = lines;
  }, []);

  /** Выгружает кэш в серию отсортированным массивом (time монотонно растёт). */
  const flushSeries = useCallback(() => {
    const data: ChartPoint[] = Array.from(mergedRef.current.entries())
      .sort(([a], [b]) => a - b)
      .map(([time, value]) => ({ time: time as UTCTimestamp, value }));
    seriesRef.current?.setData(data);
  }, []);

  /** Склейка нового чанка с кэшем без перерисовки всей серии. */
  const mergeData = useCallback(
    (points: HistoryPoint[]) => {
      let changed = false;
      for (const p of points) {
        const ts = toTimestamp(p.time);
        if (mergedRef.current.get(ts) !== p.value) {
          mergedRef.current.set(ts, p.value);
          changed = true;
        }
      }
      if (changed) {
        flushSeries();
        updatePriceLines();
      }
    },
    [flushSeries, updatePriceLines]
  );

  // ───────────────────────── Lazy-загрузчик ─────────────────────────
  /**
   * Предохранители (ТЗ «Защита от бесконечных запросов»):
   *  - не запускаем, если запрос уже идёт (`loadingRef`);
   *  - не запускаем, если достигнут `oldestAvailableDate`;
   *  - не запускаем, если видимый диапазон ещё перекрыт загруженными данными;
   *  - не запрашиваем диапазон левее уже загруженного (`from >= loadedFrom`).
   *
   * Период запроса всегда совпадает с активным (`viewport.period`) — поэтому
   * точки корректно мержатся в одну серию без артефактов.
   */
  const maybeTriggerLazyLoad = useCallback(async () => {
    const chart = chartRef.current;
    if (!chart) return;
    const vp = viewportRef.current;
    if (loadingRef.current || vp.oldestAvailableDate !== null) return;

    // Читаем реальный видимый диапазон из графика.
    const range = chart.timeScale().getVisibleRange();
    if (!range) return;
    const visibleFrom = fromTimestamp(range.from as number);
    const visibleTo = fromTimestamp(range.to as number);
    if (!needsLazyLoad(vp, visibleFrom, visibleTo)) return;

    requestLazyLoad(nextLazyLoadFrom(vp));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  /**
   * Жёсткий предел числа последовательно подгружаемых чанков за один заход
   * автодогрузки. Страхует от аномалий (напр., если `needsLazyLoad` по какой-то
   * причине не сбрасывается) — на практике цикл завершается раньше: либо данных
   * стало достаточно (`needsLazyLoad` = false), либо достигнут предел истории
   * (`oldestAvailableDate`), либо левая граница не сдвинулась.
   */
  const MAX_AUTOPRELOAD_CHAIN = 4;

  /**
   * Выполняет один lazy-запрос от `from` тем же интервалом и, если после merge
   * видимый диапазон всё ещё требует данных (сильное отдаление колёсиком),
   * немедленно тянет следующий чанк — без ожидания нового события range-change.
   * Общий путь для колесного, панорамного и фонового триггеров.
   *
   * @param from    левая граница диапазона (`till` = «сейчас», правый край прибит)
   * @param silent  не показывать индикатор «Загрузка…» (для фоновой предзагрузки)
   */
  const requestLazyLoad = useCallback(
    async (from: Date, silent = false) => {
      const vp = viewportRef.current;
      const chart = chartRef.current;
      if (loadingRef.current || vp.oldestAvailableDate !== null) return;
      if (from.getTime() >= vp.loadedFrom.getTime()) return; // не уходим левее без нужды

      loadingRef.current = true;
      if (!silent) setIsFetchingMore(true);
      try {
        let nextFrom = from;
        // Цикл автодогрузки: при сильном отдалении одного чанка не хватает, а
        // ждать нового события range-change (после merge/setData) не нужно —
        // проверяем реальный видимый диапазон и тянем дальше сразу.
        for (let i = 0; i < MAX_AUTOPRELOAD_CHAIN; i++) {
          if (nextFrom.getTime() >= vp.loadedFrom.getTime()) break;
          const chunk = await fetchDataRef.current(vp.period, nextFrom);
          if (chunk.length === 0) {
            // Пустой ответ = дальше истории нет.
            vp.oldestAvailableDate = new Date(vp.loadedFrom);
            break;
          }
          mergeData(chunk);
          const earliestTs = Math.min(...chunk.map((c) => toTimestamp(c.time)));
          vp.loadedFrom = new Date(earliestTs * 1000);

          // Хватит ли теперь данных? Читаем актуальный видимый диапазон и
          // спрашиваем `needsLazyLoad`. Предохранители (`oldestAvailableDate`)
          // проверяются внутри `needsLazyLoad` и здесь (break выше).
          if (chart && vp.oldestAvailableDate === null) {
            const range = chart.timeScale().getVisibleRange();
            if (range) {
              const visibleFrom = fromTimestamp(range.from as number);
              const visibleTo = fromTimestamp(range.to as number);
              if (!needsLazyLoad(vp, visibleFrom, visibleTo)) break; // данных достаточно
            }
          }
          nextFrom = nextLazyLoadFrom(vp);
        }
      } catch {
        // Тихо проглатываем: текущие данные остаются, пользователь может повторить.
        // Глобальный тост 5xx показывает интерсептор apiClient.
      } finally {
        loadingRef.current = false;
        if (!silent) setIsFetchingMore(false);
      }
    },
    [mergeData]
  );

  // ───────────────────────── Состояния данных ─────────────────────────
  const isLoading = mainQuery.isLoading;
  const isError = mainQuery.isError;
  const isEmpty = !mainQuery.data || mainQuery.data.length === 0;

  return (
    <Box>
      {/* Кнопки выбора периода. Цвет активной — фирменный `#3dba8d`. */}
      <Box sx={{ display: 'flex', gap: 0.5, mb: 1 }}>
        {CANONICAL_PERIODS.map((code) => {
          const active = code === period;
          return (
            <Button
              key={code}
              size="small"
              disableElevation
              variant={active ? 'contained' : 'outlined'}
              onClick={() => handleSelectPeriod(code)}
              sx={{
                minWidth: 48,
                px: 1.5,
                py: 0.5,
                fontSize: '0.75rem',
                fontWeight: 700,
                ...(active && {
                  bgcolor: '#3dba8d',
                  color: '#fff',
                  '&:hover': { bgcolor: '#35a57d' },
                }),
                ...(!active && {
                  color: 'text.secondary',
                  borderColor: 'rgba(0,0,0,0.15)',
                }),
              }}
            >
              {code}
            </Button>
          );
        })}
      </Box>

      <Box sx={{ position: 'relative', width: '100%', height: CHART_HEIGHT_PX }}>
        {/* Индикатор фоновой подгрузки старых данных. */}
        {isFetchingMore && !isLoading && (
          <Box
            sx={{
              position: 'absolute',
              top: 8,
              right: 8,
              zIndex: 3,
              display: 'flex',
              alignItems: 'center',
              gap: 1,
              background: 'rgba(255,255,255,0.9)',
              borderRadius: 1,
              px: 1,
              py: 0.5,
            }}
          >
            <CircularProgress size={14} />
            <Typography variant="caption" color="text.secondary">
              Загрузка…
            </Typography>
          </Box>
        )}

        {/*
          Контейнер графика ВСЕГДА в DOM. Если скрыть его в состояниях
          loading/error/empty, `createChart` не найдёт контейнер (цикл ожидания).
          Поэтому overlay-состояния рисуем поверх canvas.
        */}
        <div ref={containerRef} style={{ width: '100%', height: '100%' }} />

        {/* Overlay-состояния поверх контейнера. */}
        {isLoading && (
          <Overlay>
            <TableSkeleton rows={2} />
          </Overlay>
        )}
        {isError && (
          <Overlay>
            <StateError
              title="Не удалось загрузить данные графика"
              helperText="Проверьте подключение и попробуйте снова."
              error={mainQuery.error}
              onRetry={() => mainQuery.refetch()}
              retryText="Повторить"
            />
          </Overlay>
        )}
        {isEmpty && !isLoading && !isError && (
          <Overlay>
            <Typography variant="h6" color="text.secondary">
              Нет данных за период
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Возможно, биржа была закрыта (выходные/праздники).
            </Typography>
          </Overlay>
        )}
      </Box>
    </Box>
  );
}

/** Overlay-контейнер: центрирует контент поверх canvas, не блокируя mount. */
function Overlay({ children }: { children: React.ReactNode }) {
  return (
    <Box
      sx={{
        position: 'absolute',
        inset: 0,
        zIndex: 2,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 1,
        background: 'rgba(255,255,255,0.6)',
        pointerEvents: 'auto',
        px: 2,
      }}
    >
      {children}
    </Box>
  );
}

// ───────────────────────── Чистые хелперы (без состояния) ─────────────────────────

/**
 * Инлайн-стационарные стили плавающего tooltip (дата + значение под курсором).
 * Применяется через `Object.assign(... as CSSStyleDeclaration)`, т.к. узел
 * создаётся вне React и управляется напрямую в DOM. Выглядит как компактный
 * «пилл» поверх графика. БЕЗ `transform`/`left`/`top` — позицию выставляет
 * `positionTooltip` (фиксированное смещение вправо-вниз от курсора).
 */
const tooltipStyle: Partial<CSSStyleDeclaration> = {
  position: 'absolute',
  zIndex: '4',
  display: 'none',
  pointerEvents: 'none',
  whiteSpace: 'nowrap',
  padding: '6px 10px',
  borderRadius: '6px',
  background: 'rgba(255,255,255,0.95)',
  boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
  border: '1px solid rgba(0,0,0,0.08)',
  fontFamily: 'inherit',
  fontSize: '12px',
  lineHeight: '1.5',
  color: '#111111',
};

/**
 * Вертикальная пунктирная направляющая под курсором (ТЗ «Вертикальная линия»):
 * чёрный пунктир через всю высоту холста. Реализована тонким (width:0) div'ом с
 * левым пунктирным бордюром — так линия рисуется средствами CSS и гарантированно
 * не влияет на canvas/график (`pointerEvents: 'none'`, `z-index` под tooltip).
 * Высота = `top:0; bottom:0` — тянется на весь контейнер. Скрытие — `display:none`.
 */
const vLineStyle: Partial<CSSStyleDeclaration> = {
  position: 'absolute',
  top: '0',
  bottom: '0',
  width: '0',
  borderLeft: '1px dashed #000000',
  pointerEvents: 'none',
  display: 'none',
  zIndex: '3',
};

/**
 * Круглый маркер в точке пересечения линии графика и вертикальной направляющей
 * (ТЗ «Маркер»). Фирменный цвет графика `#3dba8d` + мягкое свечение вокруг.
 * Появление/исчезновение анимируется CSS (класс `.cct-marker-visible`, см.
 * `ensureOverlayStyle`) — инлайн здесь только стационарные свойства; `transform`
 * и `opacity` намеренно НЕ задаются инлайном, чтобы CSS-transition работала.
 */
const markerStyle: Partial<CSSStyleDeclaration> = {
  position: 'absolute',
  width: '10px',
  height: '10px',
  borderRadius: '50%',
  background: '#3dba8d',
  boxShadow: '0 0 0 3px rgba(61,186,141,0.2)',
  pointerEvents: 'none',
  zIndex: '4',
};

/**
 * CSS для overlay-элементов графика (tooltip + маркер). Инъекция через <style>:
 * узлы живут вне React и управляются напрямую в DOM. Один тег стиля на документ,
 * повторно не добавляется.
 *
 * `.cct-marker-visible` переключается классом (а не инлайн-стилем), чтобы
 * сработал CSS transition — плавное появление/исчезновение маркера (ТЗ
 * «плавная анимация появления»). Без transition маркер «дёргался» бы в точку.
 */
const OVERLAY_STYLE_ID = 'imitrade-chart-crosshair-style';
function ensureOverlayStyle(document: Document) {
  if (document.getElementById(OVERLAY_STYLE_ID)) return;
  const style = document.createElement('style');
  style.id = OVERLAY_STYLE_ID;
  style.textContent = [
    '[data-chart-tooltip] .cct-label{font-weight:700}',
    '.cct-marker{opacity:0;transform:translate(-50%,-50%) scale(0.6);transition:opacity 120ms ease-out,transform 120ms ease-out}',
    '.cct-marker.cct-marker-visible{opacity:1;transform:translate(-50%,-50%) scale(1)}',
  ].join('\n');
  document.head.appendChild(style);
}

/**
 * Ставит tooltip всегда справа-снизу от курсора на фиксированное смещение и не
 * двигает его относительно курсора (никаких переворотов и прижатий к краям).
 * Левый верхний угол окна = (cursorX + OFFSET, cursorY + OFFSET).
 */
const TOOLTIP_OFFSET_PX = 14;
function positionTooltip(tooltip: HTMLDivElement, cursorX: number, cursorY: number) {
  tooltip.style.left = `${cursorX + TOOLTIP_OFFSET_PX}px`;
  tooltip.style.top = `${cursorY + TOOLTIP_OFFSET_PX}px`;
}

/** Парсит ISO-строку времени из DTO в UNIX-секунды. */
function toTimestamp(iso: string): number {
  const ms = Date.parse(iso);
  return Number.isFinite(ms) ? Math.floor(ms / 1000) : 0;
}

/** Подгон видимого диапазона под стартовую глубину активного периода. */
function fitInitialVisibleRange(chart: IChartApi, vp: ViewportState) {
  const from = toTimestamp(isoFromMs(addDays(vp.loadedTo, -PERIOD_LOOKBACK_DAYS[vp.period]).getTime()));
  const to = toTimestamp(isoFromMs(vp.loadedTo.getTime()));
  try {
    chart.timeScale().setVisibleRange({ from: from as Time, to: to as Time });
  } catch {
    // setVisibleRange бросает, если данных ещё нет — игнорируем на старте.
  }
}

/** ISO-строка из ms (для консистентного парсинга в toTimestamp). */
function isoFromMs(ms: number): string {
  return new Date(ms).toISOString();
}

/** ISO-дата `yyyy-MM-dd` из Date (для стабильного ключа React Query). */
function fromDateToIso(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}
