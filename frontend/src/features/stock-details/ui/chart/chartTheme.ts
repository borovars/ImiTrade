import type { DeepPartial, ChartOptions, AreaSeriesOptions } from 'lightweight-charts';

/**
 * Визуальная тема графика цены акции.
 *
 * Требования дизайна (ТЗ «Профессиональный график цены»):
 *  - линия цвета `#3dba8d`;
 *  - плавный прозрачный градиент под линией (сверху — линия, снизу — затухание
 *    к оси X);
 *  - чистый фон: без сетки, без горизонтальных и вертикальных линий;
 *  - оси минимизированы: только временная шкала снизу и цена сбоку, без рамок.
 *
 * Числовые значения режимов (`CrosshairMode.Normal`, `LineType.Simple`)
 * прописаны литералами, т.к. это `DeepPartial`-опции и enum-импорт ради двух
 * констант излишен. Соответствие: 0 = Normal, 0 = Simple (см. typings.d.ts).
 */

/** Цвет линии графика (и акцент crosshair). */
export const CHART_LINE_COLOR = '#3dba8d';
/** Цвет текста осей — чёрный (был серый `#6b7280`). */
export const AXIS_TEXT_COLOR = '#111111';
/** Крупный шрифт осей (был дефолтный 12px, затем 14px). */
export const AXIS_FONT_SIZE = 24;

/**
 * Базовые опции графика (`createChart`): фон, отключённая сетка, отключённый
 * нативный wheel-зум (масштаб управляется вручную в `chartZoom.ts`), прибитые
 * края и скрытые границы осей.
 */
export const chartBaseOptions: DeepPartial<ChartOptions> = {
  layout: {
    background: { color: 'transparent' },
    textColor: AXIS_TEXT_COLOR,
    fontSize: AXIS_FONT_SIZE,
    fontFamily: 'inherit',
  },
  // Чистый фон: никакой сетки.
  grid: {
    horzLines: { visible: false },
    vertLines: { visible: false },
  },
  // Колесо мыши полностью отключаем на стороне библиотеки: его обрабатывает
  // наш собственный non-passive `wheel` listener на контейнере (React `onWheel`
  // — passive и не даёт вызвать `preventDefault`, из-за чего страница скроллится
  // вместе с графиком). Нативный wheel-zoom тут тоже выключен.
  // Перетаскивание ЛКМ (`pressedMouseMove`) оставляем — это и есть
  // панорамирование влево/вправо, оно же триггерит lazy-подгрузку истории.
  handleScroll: {
    mouseWheel: false,
    pressedMouseMove: true,
    horzTouchDrag: true,
    vertTouchDrag: false,
  },
  handleScale: {
    mouseWheel: false,
    pinch: false,
    axisPressedMouseMove: false,
    axisDoubleClickReset: false,
  },
  rightPriceScale: {
    borderVisible: false,
    textColor: AXIS_TEXT_COLOR,
    // Разреженные авто-метки: основная подпись цены — на price line последней
    // свечи и на min/max price lines (см. StockPriceChart). Высокий density
    // убирает лишние авто-метки между ними.
    tickMarkDensity: 10,
    ensureEdgeTickMarksVisible: true,
  },
  timeScale: {
    borderVisible: false,
    // Правый край прибит к последней свече — пустого пространства в будущее
    // не появляется. Левый край НЕ прибит (fixLeftEdge=false): пользователь
    // может перетащить график левее загруженных данных — это и есть триггер
    // lazy-подгрузки. Ограничение слева делает фронт (oldestAvailableDate).
    fixRightEdge: true,
    fixLeftEdge: false,
    rightOffset: 0,
    timeVisible: true,
    secondsVisible: false,
  },
  crosshair: {
    mode: 0, // CrosshairMode.Normal — свободное движение
    vertLine: { color: 'rgba(61,186,141,0.4)', labelBackgroundColor: CHART_LINE_COLOR },
    horzLine: { color: 'rgba(61,186,141,0.4)', labelBackgroundColor: CHART_LINE_COLOR },
  },
  localization: {
    // Полностью подавляем авто-метки ценовой шкалы (возвращаем пустые строки).
    // На оси Y остаются только подписи price lines (min/max) и текущая цена
    // серии — ровно 3 значения. Этот форматтер применяется только к авто-меткам,
    // но не к подписям price lines.
    tickmarksPriceFormatter: (prices: number[]) => prices.map(() => ''),
  },
};

/**
 * Опции Area-серии: линия цвета `#3dba8d` + прозрачный вертикальный градиент
 * под ней (сверху полупрозрачный, к оси X — полностью прозрачный).
 *
 * `priceLineVisible: true` — показывает цену последней свечи как метку на оси
 * цены (это и есть «текущая цена»); сама линия скрыта через `priceLineStyle`
 * (= 0, LineType.Simple невидим при width… нет — см. ниже). Линию текущей цены
 * делаем тонкой/незаметной, оставляя подпись на оси.
 */
export const areaSeriesOptions: DeepPartial<AreaSeriesOptions> = {
  lineColor: CHART_LINE_COLOR,
  lineWidth: 2,
  lineType: 0, // LineType.Simple — прямые отрезки, острые изломы в точках данных
  topColor: 'rgba(61,186,141,0.35)',
  bottomColor: 'rgba(61,186,141,0.0)',
  // Подпись текущей (последней) цены на оси цены.
  priceLineVisible: true,
  priceLineColor: 'rgba(17,17,17,0.35)',
  priceLineWidth: 1,
  priceLineStyle: 2, // LineStyle.Dashed
};
