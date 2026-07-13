/**
 * Символ вымышленной валюты «Дубли» (руна ᛔ).
 *
 * Добавляется в конец любого отформатированного значения через `formatMoney`,
 * поэтому автоматически появляется везде: балансы, цены, суммы сделок,
 * PnL (`formatProfitLoss` построен поверх `formatMoney`), tooltip графика.
 */
export const CURRENCY_SYMBOL = 'ᛔ';

/**
 * Единый формат денежных значений для всего приложения.
 *
 * Группировка разрядов — пробелом, дробный разделитель — точка.
 * В конце добавляется символ валюты `CURRENCY_SYMBOL`.
 * Пример: 100000.5 -> "100 000.50 ᛔ"
 */
export function formatMoney(value: number, fractionDigits = 2): string {
  const formatted = new Intl.NumberFormat('en-US', {
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  }).format(value);

  // en-US даёт запятую как разделитель групп — приводим к пробелу,
  // чтобы получить единый читаемый вид "100 000.00 ᛔ".
  return `${formatted.replace(/,/g, ' ')} ${CURRENCY_SYMBOL}`;
}

/**
 * Дата/время в читаемом формате "yyyy-mm-dd hh:mm".
 *
 * Детерминированный формат вместо браузерной локали — единый вид во всех
 * компонентах. Принимает то же, что и конструктор Date (ISO-строка, таймстамп,
 * Date). Используется, например, в истории операций (transactions).
 */
export function formatDateTime(value: number | string | Date): string {
  const date = new Date(value);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(
    date.getHours()
  )}:${pad(date.getMinutes())}`;
}

/**
 * Прибыль/убыток со знаком и цветом по теме MUI.
 *
 * Положительное значение — зелёный (success.main) с плюсом,
 * отрицательное — красный (error.main) с минусом,
 * ноль — стандартный цвет (text.primary) без знака.
 *
 * Цвета берутся из темы MUI, а не прописываются жёстко.
 */
export function formatProfitLoss(value: number): { text: string; color: string } {
  if (value > 0) {
    return { text: `+ ${formatMoney(value)}`, color: 'success.main' };
  }
  if (value < 0) {
    return { text: `− ${formatMoney(Math.abs(value))}`, color: 'error.main' };
  }
  return { text: formatMoney(value), color: 'text.primary' };
}

/**
 * Процент со знаком и цветом по теме MUI.
 *
 * Применяется к относительному изменению цены (current − average) / average × 100.
 * Положительное значение — зелёный (success.main) с плюсом, отрицательное —
 * красный (error.main) с минусом, ноль — стандартный цвет (text.primary) без знака.
 * Один знак после точки, символ «%» добавляется автоматически.
 */
export function formatPercent(value: number): { text: string; color: string } {
  const formatted = value.toFixed(1);
  if (value > 0) {
    return { text: `+ ${formatted}%`, color: 'success.main' };
  }
  if (value < 0) {
    return { text: `− ${Math.abs(value).toFixed(1)}%`, color: 'error.main' };
  }
  return { text: `${formatted}%`, color: 'text.primary' };
}
