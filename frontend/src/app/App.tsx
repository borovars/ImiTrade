import QueryProvider from '@/shared/providers/QueryProvider';
import MuiThemeProvider from '@/shared/providers/MuiThemeProvider';
import ToastProvider from '@/shared/providers/ToastProvider';
import AppRouter from './AppRouter';

export default function App() {
  return (
    <QueryProvider>
      <MuiThemeProvider>
        <ToastProvider>
          <AppRouter />
        </ToastProvider>
      </MuiThemeProvider>
    </QueryProvider>
  );
}
