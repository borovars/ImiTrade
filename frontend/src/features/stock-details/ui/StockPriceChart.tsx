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
import { useStockHistoryQuery } from '../model/useStockHistoryQuery';
import { getStockHistory } from '../api/stockHistoryApi';
import type { HistoryPeriodCode, HistoryPoint } from '../model/historyTypes';
import { StateError, TableSkeleton } from '@/shared/components';
import { formatDateTime, formatMoney } from '@/shared/utils/format';
import { AXIS_TEXT_COLOR, chartBaseOptions, areaSeriesOptions } from './chart/chartTheme';
import {
  type ViewportState,
  BACKGROUND_PRELOAD_CHUNKS,
  BACKGROUND_PRELOAD_DELAY_MS,
  CANONICAL_PERIODS,
  PERIOD_LOOKBACK_DAYS,
  addDays,
  createInitialViewport,
  debounce,
  fromTimestamp,
  needsLazyLoad,
  nextLazyLoadFrom,
} from './chart/chartZoom';

interface StockPriceChartProps {
  /** Тикер акции. Разные тикеры → независимые графики и кэш React Query. */
  ticker: string;
}

/** Точка для lightweight-charts: время в UNIX-секундах + close. */
interface ChartPoint {
  time: UTCTimestamp;
  value: number;
}

const CHART_HEIGHT_PX = 380;
/**
 * Минимальный зазор (px) между подписью текущей цены и min/max на оси Y. При
 * сближении меток подпись current скрывается — это убирает дребезг/смену меток
 * местами, когда текущая цена почти равна min или max.
 */
const MIN_LABEL_GAP_PX = 22;

/**
 * Профессиональный интерактивный график цены акции (Area, только close).
 *
 * Модель взаимодействия (стиль Т-Инвестиций / MOEX):
 *  - кнопки 1D / 1W / 1M / 1Y — выбор интервала свечи и стартовой глубины
 *    (1D=день/3мес, 1W=неделя/5мес, 1M=месяц/3года, 1Y=квартал/10лет);
 *  - перетаскивание ЛКМ — панорамирование; выход за левый край → lazy-подгрузка
 *    того же интервала (бесконечный скролл в прошлое);
 *  - колесо мыши — догрузка старых данных порциями (1D→+неделя, 1W→+2 месяца,
 *    1M→+год, 1Y→+2 года за щелчок). Не масштабирует ширину свечей, а
 *    расширяет видимый диапазон влево — как в брокерских приложениях.
 *
 * Инвариант против артефактов: вся серия всегда состоит из свечей одного
 * bucket size (текущий `period`), поэтому при lazy-merge никогда не возникает
 * пиков/провалов от смешивания интервалов.
 *
 * Визуально (по ТЗ): линия `#3dba8d` + прозрачный градиент, чистый фон без
 * сетки, правый край прибит к последней свече, resize через `autoSize`.
 */
export default function StockPriceChart({ ticker }: StockPriceChartProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const chartRef = useRef<IChartApi | null>(null);
  const seriesRef = useRef<ISeriesApi<'Area'> | null>(null);
  // Плавающее окно tooltip (дата + цена под курсором). Создаётся один раз и
  // позиционируется/скрывается через прямую запись в DOM — движение мыши не
  // должно перерисовывать React (см. readme: «пишется в DOM напрямую, без re-render»).
  const tooltipRef = useRef<HTMLDivElement | null>(null);
  // Price lines для min/max видимого диапазона (третья метка — текущая цена —
  // рисуется самой серией через `priceLineVisible`). Пересоздаются при смене
  // видимого диапазона, чтобы на оси цены всегда было ровно 3 значения.
  const priceLinesRef = useRef<IPriceLine[]>([]);

  // Viewport и кэш данных — в refs: движение мыши не должно перерисовывать React.
  const viewportRef = useRef<ViewportState>(createInitialViewport('1D'));
  const mergedRef = useRef<Map<number, number>>(new Map());
  const loadingRef = useRef(false);
  const tickerRef = useRef(ticker);
  tickerRef.current = ticker;

  // React-state — для перерисовки UI вокруг графика.
  const [period, setPeriod] = useState<HistoryPeriodCode>('1D');
  const [isFetchingMore, setIsFetchingMore] = useState(false);

  // ───────────────────────── Запросы данных ─────────────────────────
  // Главный запрос: активный период, стартовая глубина = `сейчас − lookback`.
  // Правую границу бэкенд подставляет сам (`till = LocalDate.now()`).
  const initialFrom = useMemo(() => addDays(new Date(), -PERIOD_LOOKBACK_DAYS[period]), [period]);
  const mainQuery = useStockHistoryQuery(ticker, initialFrom, period);

  // ───────────────────────── Создание графика ─────────────────────────
  // Создаётся один раз на тикер. Контейнер рендерится всегда, поэтому эффект
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

    // Плавающее окно tooltip: дата + цена под курсором. Узел живёт внутри
    // контейнера графика и управляется напрямую через DOM (без re-render).
    ensureTooltipStyle(container.ownerDocument);
    const tooltip = container.ownerDocument.createElement('div');
    tooltip.setAttribute('data-chart-tooltip', '');
    Object.assign(tooltip.style, tooltipStyle as CSSStyleDeclaration);
    container.appendChild(tooltip);
    tooltipRef.current = tooltip;

    // Подписка на НАТИВНОЕ движение мыши, а не на crosshair-события библиотеки:
    // crosshair срабатывает только когда под курсором есть свеча, и между свечами
    // окно «замирало». Нативный mousemove стреляет на каждый пиксель, поэтому окно
    // постоянно следует за курсором. По X-координате через `coordinateToTime`
    // получаем ближайшую точку данных и её цену из кэша.
    const onMove = (e: MouseEvent) => {
      const rect = container.getBoundingClientRect();
      const x = e.clientX - rect.left;
      const y = e.clientY - rect.top;
      // За пределами холста (например, над осью) — скрываем.
      if (x < 0 || y < 0 || x > rect.width || y > rect.height) {
        tooltip.style.display = 'none';
        return;
      }
      // Ближайшая точка данных к X курсора.
      const time = chart.timeScale().coordinateToTime(x);
      if (time === null) {
        tooltip.style.display = 'none';
        return;
      }
      const value = merged.get(time as number);
      if (value === undefined) {
        tooltip.style.display = 'none';
        return;
      }
      tooltip.innerHTML =
        `<div><span class="cct-label">Date:</span> ${formatDateTime(new Date((time as number) * 1000))}</div>` +
        `<div><span class="cct-label">Price:</span> ${formatMoney(value)}</div>`;
      positionTooltip(tooltip, x, y, container);
      tooltip.style.display = 'block';
    };
    const onLeave = () => {
      tooltip.style.display = 'none';
    };
    container.addEventListener('mousemove', onMove);
    container.addEventListener('mouseleave', onLeave);

    // Пересчёт min/max/current price lines + проверка lazy-подгрузки при любом
    // движении видимого диапазона (панорамирование, зум, смена данных).
    const debouncedLazy = debounce(() => maybeTriggerLazyLoad(), 300);
    const onRangeChange = () => {
      updatePriceLines();
      debouncedLazy.call();
    };
    chart.timeScale().subscribeVisibleLogicalRangeChange(onRangeChange);

    return () => {
      container.removeEventListener('mousemove', onMove);
      container.removeEventListener('mouseleave', onLeave);
      chart.timeScale().unsubscribeVisibleLogicalRangeChange(onRangeChange);
      debouncedLazy.cancel();
      tooltip.remove();
      tooltipRef.current = null;
      chart.remove();
      chartRef.current = null;
      seriesRef.current = null;
      priceLinesRef.current = [];
      // Сброс под следующий тикер.
      viewportRef.current = createInitialViewport('1D');
      merged.clear();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ticker]);

  // ───────────────────── Применение данных активного периода ─────────────
  // Перерисовка при первом получении или при смене периода: полностью заменяем
  // кэш и сбрасываем viewport под новый диапазон. Состояние loading/error/empty
  // не ломает график — контейнер всегда в DOM.
  useEffect(() => {
    const series = seriesRef.current;
    if (!series || !mainQuery.data) return;

    viewportRef.current = createInitialViewport(period);

    mergedRef.current.clear();
    for (const p of mainQuery.data) mergedRef.current.set(toTimestamp(p.time), p.close);
    flushSeries();

    // Подгон видимого диапазона под стартовую глубину (правый край = сейчас) и
    // первичная отрисовка min/max price lines.
    requestAnimationFrame(() => {
      const chart = chartRef.current;
      if (chart) fitInitialVisibleRange(chart, viewportRef.current);
      updatePriceLines();
    });

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

  // ───────────────────────── Колесо = догрузка в прошлое ─────────────────────────
  // Регистрируем НАТИВНЫЙ non-passive wheel listener, чтобы можно было вызвать
  // `preventDefault()` (React `onWheel` — passive и не блокирует скролл страницы).
  // Один «щелчок» колеса = +WHEEL_STEP_DAYS[period] дней в прошлое тем же
  // интервалом свечи (масштаб не меняем, расширяем диапазон влево).
  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;
    const onWheel = (e: WheelEvent) => {
      // Только скролл «вниз» (deltaY > 0) догружает прошлое. Скролл «вверх»
      // ничего не делает (в будущее не уходим — правый край прибит).
      if (e.deltaY <= 0) return;
      e.preventDefault();
      requestLazyLoad(nextLazyLoadFrom(viewportRef.current));
    };
    container.addEventListener('wheel', onWheel, { passive: false });
    return () => container.removeEventListener('wheel', onWheel);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ticker]);

  // ───────────────────────── Внутренние хелперы (стабильные) ─────────────────────────

  /**
   * Пересчитывает ценовые линии оси Y: min (range.from), max (range.to) и
   * текущую цену последней свечи. Все три рисуются единым путём — `createPriceLine`
   * с `lineVisible: false` (на холсте линий нет, видны только подписи-«таблетки»
   * на оси). Сама линия текущей цены у серии отключена в теме (`priceLineVisible:
   * false`) — раньше именно конфликт нативной линии серии и кастомных min/max
   * давал «дребезг» и смену меток местами при current≈min/max.
   *
   * Приоритет current (текущая цена «на переднем плане»): она рисуется ВСЕГДА.
   * min/max рисуются по реальным позициям, но если подпись min или max подходит
   * к current ближе `MIN_LABEL_GAP_PX`, эту крайнюю метку пропускаем — current
   * перекрывает её (а не расталкивается с ней встроенным overlap-резолвером,
   * который даёт «прыжки» меток). Так при current≈max на оси видно current и min;
   * при current≈min — current и max; в обычном случае — все три.
   *
   * min всегда ниже max по позиции (гарантируется самой шкалой), current — по
   * реальной цене. Текущая цена добавляется ПОСЛЕ min/max, поэтому в порядке
   * отрисовки price lines она поверх них (визуальный «передний план»).
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

    // Текущая цена = цена последней свечи (самая свежая точка кэша).
    const merged = mergedRef.current;
    let currentPrice: number | null = null;
    if (merged.size > 0) {
      let latestTs = 0;
      for (const ts of merged.keys()) if (ts > latestTs) latestTs = ts;
      currentPrice = merged.get(latestTs) ?? null;
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

    // Координаты для проверки коллизий. Текущая цена «на переднем плане»: её
    // метка рисуется всегда, а min/max пропускаются, если налезают на current.
    const coordMin = series.priceToCoordinate(range.from);
    const coordMax = series.priceToCoordinate(range.to);
    const coordCur = currentPrice !== null ? series.priceToCoordinate(currentPrice) : null;
    const tooClose = (a: number | null, b: number | null) => a !== null && b !== null && Math.abs(a - b) < MIN_LABEL_GAP_PX;

    // min/max — на «заднем плане»: рисуются, только если не перекрыты current.
    const lines: ReturnType<typeof series.createPriceLine>[] = [];
    if (coordCur === null || coordMin === null || !tooClose(coordMin, coordCur)) {
      lines.push(series.createPriceLine({ ...opts, price: range.from }));
    }
    if (coordCur === null || coordMax === null || !tooClose(coordMax, coordCur)) {
      lines.push(series.createPriceLine({ ...opts, price: range.to }));
    }
    // Текущая цена — всегда (добавляется последней → поверх min/max).
    if (currentPrice !== null && coordCur !== null) {
      lines.push(series.createPriceLine({ ...opts, price: currentPrice }));
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
        if (mergedRef.current.get(ts) !== p.close) {
          mergedRef.current.set(ts, p.close);
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
   * Выполняет один lazy-запрос от `from` тем же интервалом. Общий путь для
   * колесного, панорамного и фонового триггеров.
   *
   * @param from    левая граница диапазона (`till` = «сейчас», правый край прибит)
   * @param silent  не показывать индикатор «Загрузка…» (для фоновой предзагрузки)
   */
  const requestLazyLoad = useCallback(
    async (from: Date, silent = false) => {
      const vp = viewportRef.current;
      if (loadingRef.current || vp.oldestAvailableDate !== null) return;
      if (from.getTime() >= vp.loadedFrom.getTime()) return; // не уходим левее без нужды

      loadingRef.current = true;
      if (!silent) setIsFetchingMore(true);
      try {
        const chunk = await getStockHistory(tickerRef.current, from, vp.period);
        if (chunk.length === 0) {
          // Пустой ответ = дальше истории нет.
          vp.oldestAvailableDate = new Date(vp.loadedFrom);
        } else {
          mergeData(chunk);
          const earliestTs = Math.min(...chunk.map((c) => toTimestamp(c.time)));
          vp.loadedFrom = new Date(earliestTs * 1000);
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
              title="Не удалось загрузить историю цены"
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
 * Инлайн-стиль плавающего tooltip (дата + цена под курсором). Применяется через
 * `Object.assign(... as CSSStyleDeclaration)`, т.к. узел создаётся вне React и
 * управляется напрямую в DOM. Выглядит как компактный «пилл» поверх графика.
 * БЕЗ `transition` — плавность движения обеспечивается самим mousemove (окно
 * двигается мгновенно за курсором каждый кадр, без задержки/телепортации).
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
  transform: 'translate(-50%, calc(-100% - 12px))',
};

/**
 * CSS для жирной подписи «Date:»/«Price:» внутри tooltip. Инъекция через
 * <style> — узел tooltip живёт вне React и управляется напрямую в DOM.
 * Один тег стиля на документ, повторно не добавляется.
 */
const TOOLTIP_STYLE_ID = 'imitrade-chart-tooltip-style';
function ensureTooltipStyle(document: Document) {
  if (document.getElementById(TOOLTIP_STYLE_ID)) return;
  const style = document.createElement('style');
  style.id = TOOLTIP_STYLE_ID;
  style.textContent = '[data-chart-tooltip] .cct-label{font-weight:700}';
  document.head.appendChild(style);
}

/**
 * Ставит tooltip рядом с курсором и не даёт ему вылезти за пределы контейнера.
 * Окно позиционируется над точкой курсора (transform в `tooltipStyle` уже
 * центрирует его по X и приподнимает над курсором по Y). Здесь корректируем
 * только «X у правого края» (окно уходит влево) и «Y у верхнего края» (окно
 * уходит вниз под курсор), чтобы оно всегда оставалось в кадре.
 */
function positionTooltip(
  tooltip: HTMLDivElement,
  cursorX: number,
  cursorY: number,
  container: HTMLElement
) {
  const cw = container.clientWidth;
  const tw = tooltip.offsetWidth;
  const th = tooltip.offsetHeight;

  // По умолчанию окно центрируется над курсором (см. transform в tooltipStyle).
  let left = cursorX;
  let flipDown = false;

  // Не помещается справа — прижимаем к правому краю с отступом.
  if (cursorX + tw / 2 > cw - 8) left = cw - 8 - tw / 2;
  // Не помещается слева — прижимаем к левому краю с отступом.
  else if (cursorX - tw / 2 < 8) left = 8 + tw / 2;

  // Сверху над курсором не хватает места — показываем окно снизу под курсором.
  if (cursorY - th - 12 < 8) flipDown = true;

  tooltip.style.left = `${Math.max(8, Math.min(left, cw - 8))}px`;
  tooltip.style.top = `${flipDown ? cursorY + 16 : cursorY}px`;
  tooltip.style.transform = flipDown ? 'translate(-50%, 0)' : 'translate(-50%, calc(-100% - 12px))';
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
