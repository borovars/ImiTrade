export type UserType = 'guest' | 'auth';

/**
 * Профиль аутентифицированного пользователя.
 *
 * Повторяет поля backend `CurrentUserResponse` (`GET /api/v1/users/me`) —
 * именно этот объект кладётся в `state.user` при `SET_AUTH` после входа /
 * регистрации / bootstrap.
 */
export interface User {
  id: number;
  email: string;
  username: string;
  balance: number;
  createdAt: string;
}

export interface AuthState {
  userType: UserType | null;
  isReady: boolean;
  user: User | null;
}
