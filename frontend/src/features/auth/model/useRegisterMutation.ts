import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { register, getCurrentUser } from '../api/authApi';
import { RegisterRequest, CurrentUserResponse } from '../types/authTypes';
import { queryKeys } from '@/shared/lib/queryKeys';
import { storage } from '@/shared/lib/storage';
import { ApiError } from '@/shared/api/apiClient';
import { useAuthDispatch } from './authStore';
import { saveJwtToken } from './authService';

interface RegisterResult {
  token: string;
  user: CurrentUserResponse;
}

/**
 * Регистрация / конвертация гостя.
 *
 * В `mutationFn` пробрасываем сохранённый `guestToken` в теле запроса — это
 * ключевой момент: backend не создаёт нового пользователя, а привязывает email +
 * пароль к существующему гостю (`AuthService.register` → `convertGuestToRegistered`),
 * сохраняя баланс, портфель и историю, и начисляет +20 000. После успешной
 * конвертации гостевой токен недействителен — очищаем его.
 *
 * Далее так же, как в login: грузим профиль через `getCurrentUser()` (с
 * полученным JWT), диспатчим `SET_AUTH`, инвалидируем `account`/`portfolio`/
 * `transactions` — агрегаты гостя поменялись (баланс вырос на бонус).
 */
export function useRegisterMutation() {
  const queryClient = useQueryClient();
  const dispatch = useAuthDispatch();

  return useMutation<RegisterResult, ApiError, Omit<RegisterRequest, 'guestToken'>>({
    mutationFn: async (input) => {
      const guestToken = storage.getGuestToken() ?? undefined;
      const auth = await register({ ...input, guestToken });
      storage.setJwtToken(auth.token);
      // После конвертации гостевой токен больше не валиден — забываем его.
      storage.removeGuestToken();
      const user = await getCurrentUser();
      return { token: auth.token, user };
    },
    onSuccess: ({ token, user }) => {
      saveJwtToken(token, dispatch, user);
      queryClient.invalidateQueries({ queryKey: queryKeys.account });
      queryClient.invalidateQueries({ queryKey: queryKeys.portfolio });
      queryClient.invalidateQueries({ queryKey: queryKeys.transactions });
      toast.success(`Аккаунт создан. Добро пожаловать, ${user.username}`);
    },
    onError: (error) => {
      // 4xx (409 на дубль email/username и т.п.) показываем тостом; форма не очищается.
      toast.error(error.message);
    },
  });
}
