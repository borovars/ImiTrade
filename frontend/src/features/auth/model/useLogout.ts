import { useCallback } from 'react';
import { storage } from '@/shared/lib/storage';
import { useAuthDispatch } from './authStore';

/**
 * Выход из аккаунта (frontend-only, без обращения к backend).
 *
 * Полный reload страницы с переходом на `/dashboard`:
 *   1. `storage.clearAuth()` удаляет JWT (и guest-токен, если был).
 *   2. `RESET` сбрасывает auth-state на краткий момент до перезагрузки.
 *   3. `window.location.assign('/dashboard')` перезагружает приложение — при
 *      старте `bootstrapAuth` создаст нового гостя, React Query cache будет
 *      чист по умолчанию (новая загрузка = новое состояние), и пользователь
 *      окажется на `/dashboard` в гостевом режиме.
 */
export function useLogout() {
  const dispatch = useAuthDispatch();

  return useCallback(() => {
    storage.clearAuth();
    dispatch({ type: 'RESET' });
    window.location.assign('/dashboard');
  }, [dispatch]);
}
