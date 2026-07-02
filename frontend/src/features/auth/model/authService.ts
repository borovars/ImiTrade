import { Dispatch } from 'react';
import { storage } from '@/shared/lib/storage';
import { createGuest } from '@/shared/api/apiClient';
import { AuthAction } from './authStore';
import { User } from './types';

export function loadStoredAuth(dispatch: Dispatch<AuthAction>): void {
  const jwtToken = storage.getJwtToken();
  if (jwtToken) {
    // TODO: при наличии endpoint /account/me — загружать реального пользователя
    dispatch({ type: 'SET_AUTH', payload: { id: 'jwt', email: '' } });
    return;
  }

  const guestToken = storage.getGuestToken();
  if (guestToken) {
    dispatch({ type: 'SET_GUEST' });
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

export async function bootstrapAuth(dispatch: Dispatch<AuthAction>): Promise<void> {
  try {
    const jwtToken = storage.getJwtToken();
    if (jwtToken) {
      dispatch({ type: 'SET_AUTH', payload: { id: 'jwt', email: '' } });
      dispatch({ type: 'SET_READY' });
      return;
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

export function saveJwtToken(
  token: string,
  dispatch: Dispatch<AuthAction>,
  user?: User
): void {
  storage.setJwtToken(token);
  dispatch({
    type: 'SET_AUTH',
    payload: user ?? { id: 'jwt', email: '' },
  });
}

export function saveGuestToken(token: string, dispatch: Dispatch<AuthAction>): void {
  storage.setGuestToken(token);
  dispatch({ type: 'SET_GUEST' });
}

export function resetAuth(dispatch: Dispatch<AuthAction>): void {
  storage.clearAuth();
  dispatch({ type: 'RESET' });
}
