export type UserType = 'guest' | 'auth';

export interface User {
  id: string;
  email: string;
  name?: string;
}

export interface AuthState {
  userType: UserType | null;
  isReady: boolean;
  user: User | null;
}
