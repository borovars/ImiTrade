import { z } from 'zod';

/**
 * Валидация формы регистрации.
 *
 * Зеркалирует backend-валидацию `RegisterRequest`
 * (`src/main/java/ImiTrade/auth/dto/RegisterRequest.java`):
 *   - email — валидный, до 255 символов;
 *   - username — 3–100 символов, `^[A-Za-z0-9_.-]+$`;
 *   - password — 8–100 символов;
 *   - confirmPassword — должен совпадать с password (фронтовая проверка, на backend
 *     такого поля нет).
 *
 * Прочие бизнес-проверки (уникальность email/username, конвертация гостя) —
 * на backend, не дублируются.
 */
export const registerSchema = z
  .object({
    username: z
      .string()
      .min(3, 'Имя пользователя должно содержать минимум 3 символа')
      .max(100, 'Имя пользователя должно содержать максимум 100 символов')
      .regex(/^[A-Za-z0-9_.-]+$/, 'Допускаются буквы латиницы, цифры, _, -, .'),
    email: z
      .string()
      .min(1, 'Введите email')
      .max(255, 'Email должен содержать максимум 255 символов')
      .email('Некорректный email'),
    password: z
      .string()
      .min(8, 'Пароль должен содержать минимум 8 символов')
      .max(100, 'Пароль должен содержать максимум 100 символов'),
    confirmPassword: z
      .string()
      .min(8, 'Пароль должен содержать минимум 8 символов'),
  })
  .refine((data) => data.password === data.confirmPassword, {
    path: ['confirmPassword'],
    message: 'Пароли не совпадают',
  });

export type RegisterForm = z.infer<typeof registerSchema>;
