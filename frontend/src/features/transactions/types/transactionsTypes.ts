/**
 * Контракт истории операций (GET /api/v1/transactions).
 *
 * Соответствует backend-record TransactionResponse. Денежные значения
 * (price/totalAmount) backend сериализует из BigDecimal в JSON-число → number.
 * type — строка "BUY"/"SELL" (TransactionType.name() на backend).
 * createdAt — ISO-8601 UTC (Instant), например "2024-01-01T12:00:00Z".
 *
 * Поле stockId из ответа опущено — для таблицы истории оно не нужно.
 */
export interface Transaction {
  id: number;
  type: 'BUY' | 'SELL';
  ticker: string;
  quantity: number;
  price: number;
  totalAmount: number;
  createdAt: string;
}

/**
 * Обёртка Spring Page, которую возвращает GET /api/v1/transactions.
 *
 * В отличие от /portfolio (простой массив), здесь backend отдаёт Page:
 * по умолчанию size=20, сортировка по createdAt DESC. Метаданные
 * пагинации сохраняем для будущей готовности; таблица рендерит content.
 */
export interface TransactionPage {
  content: Transaction[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
