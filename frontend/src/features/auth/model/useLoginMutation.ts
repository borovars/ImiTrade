import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { login, getCurrentUser } from '../api/authApi';
import { LoginRequest, CurrentUserResponse } from '../types/authTypes';
import { queryKeys } from '@/shared/lib/queryKeys';
import { storage } from '@/shared/lib/storage';
import { ApiError } from '@/shared/api/apiClient';
import { useAuthDispatch } from './authStore';
import { saveJwtToken } from './authService';

interface LoginResult {
  token: string;
  user: CurrentUserResponse;
}

/**
 * Вход пользователя.
 *
 * В `mutationFn` сначала логинимся (получаем JWT), сохраняем токен в storage,
 * чтобы follow-up `getCurrentUser()` прошёл с `Authorization: Bearer`, и грузим
 * профиль. В `onSuccess` диспатчим `SET_AUTH` и инвалидируем затронутые сущности
 * (`account`/`portfolio`/`transactions`) — данные пользователя нужно
 * перетянуть с backend (после входа это уже его баланс/портфель/история).
 *
 * Образец инвалидаций — `features/trading/model/useBuyStockMutation.ts`.
 */
export function useLoginMutation() {
  const queryClient = useQueryClient();
  const dispatch = useAuthDispatch();

  return useMutation<LoginResult, ApiError, LoginRequest>({
    mutationFn: async (input) => {
      const auth = await login(input);
      storage.setJwtToken(auth.token);
      const user = await getCurrentUser();
      return { token: auth.token, user };
    },
    onSuccess: ({ token, user }) => {
      saveJwtToken(token, dispatch, user);
      queryClient.invalidateQueries({ queryKey: queryKeys.account });
      queryClient.invalidateQueries({ queryKey: queryKeys.portfolio });
      queryClient.invalidateQueries({ queryKey: queryKeys.transactions });
      toast.success(`Welcome back, ${user.username}`);
    },
    onError: (error) => {
      // 4xx (включая 401 на неверный пароль) показываем тостом; форма не очищается.
      toast.error(error.message);
    },
  });
}
