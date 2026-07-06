/**
 * Единый формат денежных значений для всего приложения.
 *
 * Группировка разрядов — пробелом, дробный разделитель — точка.
 * Пример: 100000.5 -> "100 000.50"
 *
 * Без локализации валюты: символ не добавляется.
 */
export function formatMoney(value: number, fractionDigits = 2): string {
  const formatted = new Intl.NumberFormat('en-US', {
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  }).format(value);

  // en-US даёт запятую как разделитель групп — приводим к пробелу,
  // чтобы получить единый читаемый вид "100 000.00".
  return formatted.replace(/,/g, ' ');
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
