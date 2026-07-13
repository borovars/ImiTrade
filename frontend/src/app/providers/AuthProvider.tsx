import { ReactNode, useEffect } from 'react';
import { AuthStateProvider, useAuth } from '@/features/auth/model/authStore';
import { bootstrapAuth } from '@/features/auth/model/authService';

function AuthBootstrap({ children }: { children: ReactNode }) {
  const { state, dispatch } = useAuth();

  useEffect(() => {
    if (!state.isReady) {
      bootstrapAuth(dispatch);
    }
  }, [dispatch, state.isReady]);

  if (!state.isReady) {
    return null;
  }

  return <>{children}</>;
}

export default function AuthProvider({ children }: { children: ReactNode }) {
  return (
    <AuthStateProvider>
      <AuthBootstrap>{children}</AuthBootstrap>
    </AuthStateProvider>
  );
}
