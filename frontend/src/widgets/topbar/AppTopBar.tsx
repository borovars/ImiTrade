import { AppBar, Toolbar, Typography, Box, Button, Chip } from '@mui/material';
import { LogIn, UserPlus, LogOut } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useAuth } from '@/features/auth/model/authStore';
import { useLogout } from '@/features/auth/model/useLogout';

interface AppTopBarProps {
  drawerWidth: number;
}

/**
 * Топбар.
 *
 * Навигация зависит от режима:
 *   - гость → Sign In + Create Account (ссылки на `/login` и `/register`);
 *   - залогинен → имя пользователя + кнопка Logout.
 *
 * Logout выполняется без перезагрузки страницы (`useLogout`): очищает storage,
 * React Query cache и переводит приложение в гостевой режим.
 */
export default function AppTopBar({ drawerWidth }: AppTopBarProps) {
  const { state } = useAuth();
  const logout = useLogout();

  return (
    <AppBar
      position="fixed"
      sx={{
        width: { sm: `calc(100% - ${drawerWidth}px)` },
        ml: { sm: `${drawerWidth}px` },
      }}
    >
      <Toolbar sx={{ display: 'flex', justifyContent: 'space-between' }}>
        <Typography variant="h6" noWrap component="div">
          ImiTrade
        </Typography>

        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Chip
            label={state.userType === 'auth' ? 'User' : 'Guest'}
            color={state.userType === 'auth' ? 'primary' : 'default'}
            size="small"
          />

          {state.userType === 'auth' ? (
            <>
              {state.user?.username && (
                <Typography variant="body2" noWrap sx={{ maxWidth: 160 }}>
                  {state.user.username}
                </Typography>
              )}
              <Button
                color="inherit"
                startIcon={<LogOut size={18} />}
                onClick={logout}
                size="small"
              >
                Logout
              </Button>
            </>
          ) : (
            <>
              <Button
                color="inherit"
                component={Link}
                to="/login"
                startIcon={<LogIn size={18} />}
                size="small"
              >
                Sign In
              </Button>
              <Button
                color="inherit"
                component={Link}
                to="/register"
                startIcon={<UserPlus size={18} />}
                size="small"
              >
                Create Account
              </Button>
            </>
          )}
        </Box>
      </Toolbar>
    </AppBar>
  );
}
