import { z } from 'zod';

/**
 * Валидация формы входа.
 *
 * Проверяем только обязательность и формат email — пароль любую непустую строку
 * примет сам backend и вернёт 401 при неверных данных (без раскрытия, чем именно
 * ошибся пользователь, см. `AuthService.login`).
 */
export const loginSchema = z.object({
  email: z.string().min(1, 'Введите email').email('Некорректный email'),
  password: z.string().min(1, 'Введите пароль'),
});

export type LoginForm = z.infer<typeof loginSchema>;
