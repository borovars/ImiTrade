import { z } from 'zod';

/**
 * Валидация количества в форме сделки.
 *
 * `coerce` нужен, потому что MUI TextField возвращает строку.
 * Запрещаем ≤0, дробные и пустые значения. Остальные бизнес-проверки
 * (баланс, наличие позиции и т.д.) выполняет backend — не дублируем.
 */
export const quantitySchema = z.object({
  quantity: z.coerce
    .number({ message: 'Enter a quantity' })
    .int('Quantity must be an integer')
    .positive('Quantity must be greater than 0'),
});

export type QuantityForm = z.infer<typeof quantitySchema>;
