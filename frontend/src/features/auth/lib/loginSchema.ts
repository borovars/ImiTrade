import { z } from 'zod';

/**
 * Валидация формы входа.
 *
 * Проверяем только обязательность и формат email — пароль любую непустую строку
 * примет сам backend и вернёт 401 при неверных данных (без раскрытия, чем именно
 * ошибся пользователь, см. `AuthService.login`).
 */
export const loginSchema = z.object({
  email: z.string().min(1, 'Email is required').email('Enter a valid email'),
  password: z.string().min(1, 'Password is required'),
});

export type LoginForm = z.infer<typeof loginSchema>;
