import AuthProvider from '@/app/providers/AuthProvider';
import QueryProvider from '@/app/providers/queryProvider';
import MuiThemeProvider from '@/shared/providers/MuiThemeProvider';
import ToastProvider from '@/shared/providers/ToastProvider';
import AppRouter from './AppRouter';

export default function App() {
  return (
    <AuthProvider>
      <QueryProvider>
        <MuiThemeProvider>
          <ToastProvider>
            <AppRouter />
          </ToastProvider>
        </MuiThemeProvider>
      </QueryProvider>
    </AuthProvider>
  );
}
