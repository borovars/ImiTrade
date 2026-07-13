import type { HistoryPeriodCode } from '../../model/historyTypes';

/**
 * Модель взаимодействия с графиком (стиль Т-Инвестиций / MOEX).
 *
 * Каждая кнопка периода (1D / 1W / 1M / 1Y) — это и фиксированный интервал свечи,
 * и стартовая глубина истории. И панорамирование (ЛКМ-драг), и колесо мыши
 * догружают более старые данные **тем же интервалом** — поэтому вся серия всегда
 * состоит из свечей одного bucket size, без артефактов (пиков/провалов) от
 * смешивания интервалов.
 */

const DAY_MS = 24 * 60 * 60 * 1000;

/**
 * Стартовая глубина (lookback) для каждой кнопки в днях от «сейчас».
 *
 * 1D → минус 3 месяца, 1W → минус 5 месяцев, 1M → минус 3 года, 1Y → минус 10 лет.
 * Должно совпадать с backend `HistoryPeriod.lookback`, чтобы стартовый запрос
 * одной кнопкой возвращал ровно дефолтный диапазон.
 */
export const PERIOD_LOOKBACK_DAYS: Record<HistoryPeriodCode, number> = {
  '1D': 90, // ~3 месяца
  '1W': 150, // ~5 месяцев
  '1M': 365 * 3, // ~3 года
  '1Y': 365 * 10, // ~10 лет
};

/**
 * Шаг одного «щелчка» колеса мыши: сколько дней добавляется в прошлое за один
 * скролл. Подобрано под каждый интервал, чтобы движение было ощутимым, но не
 * прыжкообразным (как в Т-Инвестициях: 1D→+неделя, 1W→+2 месяца и т.д.).
 */
export const WHEEL_STEP_DAYS: Record<HistoryPeriodCode, number> = {
  '1D': 7, // неделя
  '1W': 60, // ~2 месяца
  '1M': 365, // год
  '1Y': 730, // ~2 года
};

/**
 * Размер одного lazy-чанка в днях. Запрос всегда просит диапазон `[from, now]`,
 * но `from` сдвигается кратно этому размеру, поэтому фактически новой порцией
 * данных служат свечи за `LAZY_CHUNK_DAYS`. Чанки больше `WHEEL_STEP` — один
 * запрос покрывает несколько «щелчков» колеса, и в кэше всегда есть запас.
 */
export const LAZY_CHUNK_DAYS: Record<HistoryPeriodCode, number> = {
  '1D': 365, // ~год дневных свечей за один чанк
  '1W': 730, // ~2 года недельных
  '1M': 365 * 5, // ~5 лет месячных
  '1Y': 365 * 20, // ~20 лет квартальных
};

/**
 * Глубина фоновой предзагрузки после первого рендера: сколько дополнительных
 * чанков уходим в прошлое сразу (в фоне), чтобы к моменту прокрутки данные уже
 * были. Один чанк — баланс между запасом в кэше и нагрузкой на MOEX: при
 * открытии/переключении нескольких акций бóльшая предзагрузка приводила к
 * таймаутам MOEX (он лимитирует число соединений с одного IP).
 */
export const BACKGROUND_PRELOAD_CHUNKS = 1;

/**
 * Задержка перед фоновой предзагрузкой (мс). Даёт основному запросу и рендеру
 * завершиться без конкуренции за соединение с MOEX, и не мешает первичному
 * UX-восприятию страницы.
 */
export const BACKGROUND_PRELOAD_DELAY_MS = 1500;

/**
 * Порядок кнопок выбора периода (слева направо).
 */
export const CANONICAL_PERIODS: HistoryPeriodCode[] = ['1D', '1W', '1M', '1Y'];

/**
 * Запас для предзагрузки: триггерим lazy-запрос, когда видимый левый край
 * приближается к границе загруженных данных ближе чем на эту долю от ширины
 * видимого диапазона. Так при панорамировании данные успевают прийти.
 */
const PRELOAD_FRACTION = 0.5;

/**
 * Состояние viewport'а графика.
 *
 * Хранится в `useRef` (а не в React-state): движение мыши не должно
 * перерисовывать компонент. React-state включается только для UI вокруг графика.
 */
export interface ViewportState {
  /** Активный интервал свечи (он же — кнопка периода). */
  period: HistoryPeriodCode;
  /** Самая ранняя загруженная дата (расширяется влево по мере lazy-load). */
  loadedFrom: Date;
  /** Самая поздняя загруженная дата (всегда «сейчас», правый край прибит). */
  loadedTo: Date;
  /** Достигнутый предел истории слева; `null`, пока неизвестен. */
  oldestAvailableDate: Date | null;
}

/**
 * Создаёт начальное состояние viewport'а для выбранного периода: загруженный
 * диапазон = последние `PERIOD_LOOKBACK_DAYS[period]` дней.
 */
export function createInitialViewport(
  period: HistoryPeriodCode,
  now: Date = new Date()
): ViewportState {
  return {
    period,
    loadedFrom: addDays(now, -PERIOD_LOOKBACK_DAYS[period]),
    loadedTo: now,
    oldestAvailableDate: null,
  };
}

/**
 * Нужно ли догружать историю слева.
 *
 * Главный предохранитель бесконечных запросов. Условие: достигнутый предел
 * истории ещё неизвестен (`oldestAvailableDate === null`) И видимый левый край
 * подошёл к границе загруженных данных ближе чем на `PRELOAD_FRACTION` от ширины
 * видимого диапазона. `visibleFrom`/`visibleTo` — текущий видимый диапазон
 * графика (во времени), который сообщает lightweight-charts.
 */
export function needsLazyLoad(
  viewport: ViewportState,
  visibleFrom: Date,
  visibleTo: Date
): boolean {
  if (viewport.oldestAvailableDate !== null) return false;
  const visibleSpan = Math.max(visibleTo.getTime() - visibleFrom.getTime(), 1);
  const margin = visibleSpan * PRELOAD_FRACTION;
  // Триггер: видимый левый край зашёл в зону `margin` у загруженного левого края.
  return visibleFrom.getTime() - margin <= viewport.loadedFrom.getTime();
}

/**
 * Какую левую границу (`from`) запросить для следующего чанка. Берём от текущей
 * `loadedFrom` и уходим влево на один `LAZY_CHUNK_DAYS[period]` — точки того же
 * интервала корректно мержатся в серию. Чанки крупнее шага колеса, поэтому один
 * запрос покрывает несколько прокруток, и в кэше остаётся запас.
 */
export function nextLazyLoadFrom(viewport: ViewportState): Date {
  return addDays(viewport.loadedFrom, -LAZY_CHUNK_DAYS[viewport.period]);
}

// ---------------------------------------------------------------------------
// Дата-хелперы (без внешних зависимостей).
// ---------------------------------------------------------------------------

/** Смещает дату на `days` дней (отрицательное — в прошлое). */
export function addDays(date: Date, days: number): Date {
  const d = new Date(date);
  d.setDate(d.getDate() + days);
  return d;
}

/** Парсит UNIX-секунды в дату. */
export function fromTimestamp(ts: number): Date {
  return new Date(ts * 1000);
}

/** Преобразует дату в UNIX-секунды. */
export function toTimestamp(date: Date): number {
  return Math.floor(date.getTime() / 1000);
}

/** День в миллисекундах (для арифметики диапазонов). */
export const MS_PER_DAY = DAY_MS;

/**
 * Простой debounce: вызывает `fn` через `delay` мс после последнего вызова.
 * Используется для троттлинга событий движения графика
 * (ТЗ: «debounce/throttle событий движения»).
 */
export function debounce<T extends (...args: never[]) => void>(
  fn: T,
  delay: number
): { call: (...args: Parameters<T>) => void; cancel: () => void } {
  let timer: ReturnType<typeof setTimeout> | null = null;
  return {
    call(...args: Parameters<T>) {
      if (timer !== null) clearTimeout(timer);
      timer = setTimeout(() => {
        timer = null;
        fn(...args);
      }, delay);
    },
    cancel() {
      if (timer !== null) {
        clearTimeout(timer);
        timer = null;
      }
    },
  };
}
