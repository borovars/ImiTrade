import { z } from 'zod';

/**
 * Валидация количества лотов в форме сделки.
 *
 * `coerce` нужен, потому что MUI TextField возвращает строку.
 * Запрещаем ≤0, дробные и пустые значения. Кратность lotSize не валидируется
 * на фронте — backend гарантированно вычисляет `quantity = lots × lotSize`.
 * Остальные бизнес-проверки (баланс, наличие позиции и т.д.) тоже на backend.
 */
export const lotsSchema = z.object({
  lots: z.coerce
    .number({ message: 'Enter a number of lots' })
    .int('Lots must be an integer')
    .positive('Lots must be greater than 0'),
});

export type LotsForm = z.infer<typeof lotsSchema>;
