import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactNode, useState } from 'react';

function createQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 5 * 60 * 1000,
        refetchOnWindowFocus: false,
        retry: 1,
      },
    },
  });
}

interface QueryProviderProps {
  children: ReactNode;
}

export default function QueryProvider({ children }: QueryProviderProps) {
  const [queryClient] = useState(() => createQueryClient());

  return (
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  );
}
