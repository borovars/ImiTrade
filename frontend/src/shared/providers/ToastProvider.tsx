import { Toaster } from 'sonner';
import { ReactNode } from 'react';

interface ToastProviderProps {
  children: ReactNode;
}

export default function ToastProvider({ children }: ToastProviderProps) {
  return (
    <>
      {children}
      <Toaster position="top-right" richColors />
    </>
  );
}
