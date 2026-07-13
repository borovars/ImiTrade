import { Dispatch } from 'react';
import { storage } from '@/shared/lib/storage';
import { createGuest } from '@/shared/api/apiClient';
import { getCurrentUser } from '../api/authApi';
import { AuthAction } from './authStore';
import { User } from './types';

/**
 * Bootstrap при старте приложения.
 *
 * Если есть сохранённый JWT — грузим реальный профиль через `GET /users/me`
 * (раньше тут стояла заглушка `{ id: 'jwt', email: '' }`). При протухшем /
 * невалидном JWT — отбрасываем его и проваливаемся в guest-ветку.
 *
 * Если есть guest-токен — `SET_GUEST`. Иначе автоматически создаём гостя
 * (главная концепция приложения: можно пользоваться без регистрации).
 */
export async function bootstrapAuth(dispatch: Dispatch<AuthAction>): Promise<void> {
  try {
    const jwtToken = storage.getJwtToken();
    if (jwtToken) {
      try {
        const user = await getCurrentUser();
        dispatch({ type: 'SET_AUTH', payload: user });
        dispatch({ type: 'SET_READY' });
        return;
      } catch (error) {
        // JWT протух или невалиден — забываем его и работаем как гость.
        console.warn('Stored JWT is invalid, falling back to guest mode', error);
        storage.removeJwtToken();
      }
    }

    const guestToken = storage.getGuestToken();
    if (guestToken) {
      dispatch({ type: 'SET_GUEST' });
      dispatch({ type: 'SET_READY' });
      return;
    }

    await initGuestIfNeeded(dispatch);
  } catch (error) {
    console.error('Auth bootstrap failed:', error);
  } finally {
    dispatch({ type: 'SET_READY' });
  }
}

export async function initGuestIfNeeded(dispatch: Dispatch<AuthAction>): Promise<void> {
  try {
    await createGuest();
    dispatch({ type: 'SET_GUEST' });
  } catch (error) {
    console.error('Failed to create guest, retrying once...', error);
    try {
      await createGuest();
      dispatch({ type: 'SET_GUEST' });
    } catch (retryError) {
      console.error('Guest creation retry failed:', retryError);
    }
  }
}

export function saveJwtToken(token: string, dispatch: Dispatch<AuthAction>, user: User): void {
  storage.setJwtToken(token);
  dispatch({ type: 'SET_AUTH', payload: user });
}

export function saveGuestToken(token: string, dispatch: Dispatch<AuthAction>): void {
  storage.setGuestToken(token);
  dispatch({ type: 'SET_GUEST' });
}

export function resetAuth(dispatch: Dispatch<AuthAction>): void {
  storage.clearAuth();
  dispatch({ type: 'RESET' });
}
