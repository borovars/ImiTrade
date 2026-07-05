import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { toast } from 'sonner';
import { storage } from '@/shared/lib/storage';
import { ApiError, normalizeApiError } from '@/shared/lib/apiError';
import { API_ENDPOINTS } from './endpoints';
import { GuestResponse } from './types';

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});

// Request interceptor: автоматическое определение режима аутентификации
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const jwtToken = storage.getJwtToken();
    const guestToken = storage.getGuestToken();

    if (jwtToken) {
      config.headers.set('Authorization', `Bearer ${jwtToken}`);
    } else if (guestToken) {
      config.headers.set('X-Guest-Token', guestToken);
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor: обработка ошибок
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    const normalizedError = normalizeApiError(error);
    const requestUrl = error.config?.url ?? '';

    if (normalizedError.status === 401) {
      // Жёсткая перезагрузка на 401 вызывает бесконечный цикл, если запрос
      // сам по себе создаёт гостя (без него никакой авторизации нет).
      // Поэтому перезагружаем только когда ранее был сохранённый токен —
      // то есть 401 реально означает «токен протух».
      //
      // Эндпоинты входа/регистрации тоже исключаем: 401 на /auth/login (неверный
      // пароль) и 409-на- /auth/register не должны чистить гостевую сессию и
      // перезагружать страницу — ошибку покажет форма через ApiError.
      const isAuthEndpoint =
        requestUrl.includes(API_ENDPOINTS.AUTH.LOGIN) ||
        requestUrl.includes(API_ENDPOINTS.AUTH.REGISTER);
      const hadToken = Boolean(storage.getJwtToken() || storage.getGuestToken());
      storage.clearAuth();
      if (hadToken && !requestUrl.includes(API_ENDPOINTS.GUEST) && !isAuthEndpoint) {
        window.location.href = '/';
      }
    } else if (normalizedError.status === 403) {
      toast.error(normalizedError.message || 'Доступ запрещён');
    } else if (normalizedError.status >= 500) {
      toast.error('Ошибка сервера. Попробуйте позже.');
    }

    return Promise.reject(normalizedError);
  }
);

// Создание гостевого пользователя
export async function createGuest(): Promise<string> {
  try {
    const response = await apiClient.post<GuestResponse>(API_ENDPOINTS.GUEST);
    const { guestToken } = response.data;
    storage.setGuestToken(guestToken);
    return guestToken;
  } catch (error) {
    const normalizedError = normalizeApiError(error);
    throw normalizedError;
  }
}

export default apiClient;
export { ApiError };
