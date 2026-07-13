import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import { ReactNode } from 'react';
import appTheme from '@/shared/styles/theme';

interface MuiThemeProviderProps {
  children: ReactNode;
}

export default function MuiThemeProvider({ children }: MuiThemeProviderProps) {
  return (
    <ThemeProvider theme={appTheme}>
      <CssBaseline />
      {children}
    </ThemeProvider>
  );
}
