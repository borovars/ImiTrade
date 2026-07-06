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
      .min(3, 'Username must be at least 3 characters')
      .max(100, 'Username must be at most 100 characters')
      .regex(/^[A-Za-z0-9_.-]+$/, 'Only letters, digits, _, -, . are allowed'),
    email: z
      .string()
      .min(1, 'Email is required')
      .max(255, 'Email must be at most 255 characters')
      .email('Enter a valid email'),
    password: z
      .string()
      .min(8, 'Password must be at least 8 characters')
      .max(100, 'Password must be at most 100 characters'),
    confirmPassword: z
      .string()
      .min(8, 'Password must be at least 8 characters'),
  })
  .refine((data) => data.password === data.confirmPassword, {
    path: ['confirmPassword'],
    message: 'Passwords do not match',
  });

export type RegisterForm = z.infer<typeof registerSchema>;
