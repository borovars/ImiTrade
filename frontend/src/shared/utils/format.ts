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
