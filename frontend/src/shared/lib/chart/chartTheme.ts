import type { DeepPartial, ChartOptions, AreaSeriesOptions } from 'lightweight-charts';

/**
 * Визуальная тема линейного графика (Area, только close/value).
 *
 * Переиспользуется и графиком цены акции (`features/stock-details`), и графиком
 * стоимости портфеля (`features/portfolio`). Общий стиль (ТЗ «Профессиональный
 * график»):
 *  - линия цвета `#3dba8d`;
 *  - плавный прозрачный градиент под линией (сверху — линия, снизу — затухание
 *    к оси X);
 *  - чистый фон: без сетки, без горизонтальных и вертикальных линий;
 *  - оси минимизированы: только временная шкала снизу и значение сбоку, без рамок.
 *
 * Числовые значения режимов (`CrosshairMode.Normal`, `LineType.Simple`)
 * прописаны литералами, т.к. это `DeepPartial`-опции и enum-импорт ради двух
 * констант излишен. Соответствие: 0 = Normal, 0 = Simple (см. typings.d.ts).
 */

/** Цвет линии графика (и акцент crosshair). */
export const CHART_LINE_COLOR = '#3dba8d';
/** Цвет текста осей — чёрный (был серый `#6b7280`). */
export const AXIS_TEXT_COLOR = '#111111';
/** Крупный шрифт осей (был дефолтный 12px, затем 14px, 24px). */
export const AXIS_FONT_SIZE = 20;
/**
 * Семейство шрифта осей. ВАЖНО: НЕ `'inherit'` — lightweight-charts собирает
 * canvas-строку шрифта как `` `${size}px ${family}` `` (`makeFont`), а Canvas 2D
 * API не понимает CSS-ключевое слово `inherit` в свойстве `font`, считает строку
 * невалидной и молча откатывается к дефолту canvas (~12px). Из-за этого любое
 * значение `fontSize` игнорировалось — берём реальное системное семейство
 * (совпадает с библиотечным дефолтом `defaultFontFamily`).
 */
export const AXIS_FONT_FAMILY = `-apple-system, BlinkMacSystemFont, 'Trebuchet MS', Roboto, Ubuntu, sans-serif`;

/**
 * Базовые опции графика (`createChart`): фон, отключённая сетка, зум колесом
 * относительно курсора (встроенный `handleScale.mouseWheel`), прибитые края и
 * скрытые границы осей.
 */
export const chartBaseOptions: DeepPartial<ChartOptions> = {
  layout: {
    background: { color: 'transparent' },
    textColor: AXIS_TEXT_COLOR,
    fontSize: AXIS_FONT_SIZE,
    fontFamily: AXIS_FONT_FAMILY,
  },
  // Чистый фон: никакой сетки.
  grid: {
    horzLines: { visible: false },
    vertLines: { visible: false },
  },
  // Колесо мыши как скролл (панорамирование по шкале времени) отключаем — иначе
  // оно конкурирует со зумом. Перетаскивание ЛКМ (`pressedMouseMove`) — основной
  // способ панорамирования влево/вправо; при выходе за левый край оно же
  // триггерит lazy-подгрузку истории (через `subscribeVisibleLogicalRangeChange`).
  handleScroll: {
    mouseWheel: false,
    pressedMouseMove: true,
    horzTouchDrag: true,
    vertTouchDrag: false,
  },
  // Колесо мыши ВКЛЮЧЕНО как зум силами библиотеки: lightweight-charts зумит
  // логическую шкалу времени относительно позиции курсора (как в торговых
  // терминалах), плавно и с собственным preventDefault — страница вне графика
  // при этом скроллится как обычно. Раньше здесь был `false` + отдельный
  // non-passive `wheel` listener, догружавший прошлое; теперь колесо только
  // масштабирует, а догрузка работает через ЛКМ-pan и фоновую предзагрузку.
  handleScale: {
    mouseWheel: true,
    pinch: false,
    axisPressedMouseMove: false,
    axisDoubleClickReset: false,
  },
  rightPriceScale: {
    borderVisible: false,
    textColor: AXIS_TEXT_COLOR,
    // Разреженные авто-метки: основная подпись значения — на price line последней
    // точки и на min/max price lines (см. PriceLineChart). Высокий density
    // убирает лишние авто-метки между ними.
    tickMarkDensity: 10,
    ensureEdgeTickMarksVisible: true,
  },
  timeScale: {
    borderVisible: false,
    // Правый край прибит к последней точке — пустого пространства в будущее
    // не появляется. Левый край НЕ прибит (fixLeftEdge=false): пользователь
    // может перетащить график левее загруженных данных — это и есть триггер
    // lazy-подгрузки (только при lazyLoad=true). Ограничение слева делает фронт
    // (oldestAvailableDate).
    fixRightEdge: true,
    fixLeftEdge: false,
    rightOffset: 0,
    timeVisible: true,
    secondsVisible: false,
  },
  // Crosshair-движок оставляем включённым (mode=0, Normal — свободное движение),
  // но полностью прячем его визуал: пунктирные линии и подписи на осях при
  // наведении не рисуются. Сами события crosshair (subscribeCrosshairMove) всё
  // равно нужны — по ним строится плавающее окно tooltip в PriceLineChart.
  crosshair: {
    mode: 0, // CrosshairMode.Normal — свободное движение
    vertLine: { visible: false, labelVisible: false },
    horzLine: { visible: false, labelVisible: false },
  },
  localization: {
    // Полностью подавляем авто-метки ценовой шкалы (возвращаем пустые строки).
    // На оси Y остаются только подписи price lines (min/max) и текущее значение
    // серии — ровно 3 значения. Этот форматтер применяется только к авто-меткам,
    // но не к подписям price lines.
    tickmarksPriceFormatter: (prices: number[]) => prices.map(() => ''),
  },
};

/**
 * Опции Area-серии: линия цвета `#3dba8d` + прозрачный вертикальный градиент
 * под ней (сверху полупрозрачный, к оси X — полностью прозрачный).
 *
 * `priceLineVisible: false` — нативную линию последней точки полностью отключаем.
 * Подпись текущего значения на оси Y рисуется отдельной ценовой линией
 * (`lineVisible: false`) в `updatePriceLines` — единый рендер-путь с min/max
 * исключает дребезг меток при наложении current≈min/max.
 *
 * `crosshairMarkerVisible: false` — отключаем встроенный crosshair-маркер
 * библиотеки. Свою точку рисуем отдельным DOM-overlay в `PriceLineChart`.
 */
export const areaSeriesOptions: DeepPartial<AreaSeriesOptions> = {
  lineColor: CHART_LINE_COLOR,
  lineWidth: 3,
  lineType: 0, // LineType.Simple — прямые отрезки, острые изломы в точках данных
  topColor: 'rgba(61,186,141,0.35)',
  bottomColor: 'rgba(61,186,141,0.0)',
  // Нативную линию + подпись текущего значения отключаем — подпись рисуем сами
  // (см. updatePriceLines в PriceLineChart), чтобы убрать пунктир и дребезг.
  priceLineVisible: false,
  // Нативный crosshair-маркер отключаем — рисуем свой overlay (см. onMove в
  // PriceLineChart), иначе при зуме появится вторая «точка» поверх нашей.
  crosshairMarkerVisible: false,
};
