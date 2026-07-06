import apiClient from '@/shared/api/apiClient';
import { API_ENDPOINTS } from '@/shared/api/endpoints';
import type {
  AuthResponse,
  CurrentUserResponse,
  LoginRequest,
  RegisterRequest,
} from '../types/authTypes';

/**
 * API-обёртки над `apiClient` для auth-фичи.
 *
 * Образец — `features/account/api/accountApi.ts`: функция принимает типизированный
 * payload, дёргает `apiClient` и разворачивает axios-ответ в типизированный payload.
 * Все пути берутся из `API_ENDPOINTS` — без строковых литералов.
 */

/** `POST /api/v1/auth/login` — вход, возвращает JWT. */
export async function login(data: LoginRequest): Promise<AuthResponse> {
  const response = await apiClient.post<AuthResponse>(API_ENDPOINTS.AUTH.LOGIN, data);
  return response.data;
}

/** `POST /api/v1/auth/register` — регистрация / конвертация гостя, возвращает JWT. */
export async function register(data: RegisterRequest): Promise<AuthResponse> {
  const response = await apiClient.post<AuthResponse>(API_ENDPOINTS.AUTH.REGISTER, data);
  return response.data;
}

/** `GET /api/v1/users/me` — профиль текущего пользователя (требует JWT). */
export async function getCurrentUser(): Promise<CurrentUserResponse> {
  const response = await apiClient.get<CurrentUserResponse>(API_ENDPOINTS.USERS.ME);
  return response.data;
}
