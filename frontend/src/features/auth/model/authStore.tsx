import { createContext, useContext, useReducer, Dispatch, ReactNode } from 'react';
import { AuthState, User } from './types';

export const initialAuthState: AuthState = {
  userType: null,
  isReady: false,
  user: null,
};

export type AuthAction =
  | { type: 'SET_GUEST' }
  | { type: 'SET_AUTH'; payload: User }
  | { type: 'RESET' }
  | { type: 'SET_READY' };

export function authReducer(state: AuthState, action: AuthAction): AuthState {
  switch (action.type) {
    case 'SET_GUEST':
      return { ...state, userType: 'guest', user: null };
    case 'SET_AUTH':
      return { ...state, userType: 'auth', user: action.payload };
    case 'RESET':
      return { ...state, userType: null, user: null };
    case 'SET_READY':
      return { ...state, isReady: true };
    default:
      return state;
  }
}

interface AuthContextValue {
  state: AuthState;
  dispatch: Dispatch<AuthAction>;
}

export const AuthContext = createContext<AuthContextValue | null>(null);

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}

export function useAuthState(): AuthState {
  return useAuth().state;
}

export function useAuthDispatch(): Dispatch<AuthAction> {
  return useAuth().dispatch;
}

export function AuthStateProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(authReducer, initialAuthState);

  return (
    <AuthContext.Provider value={{ state, dispatch }}>
      {children}
    </AuthContext.Provider>
  );
}
