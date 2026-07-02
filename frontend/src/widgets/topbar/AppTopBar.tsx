import { AppBar, Toolbar, Typography, Box, Button, Chip } from '@mui/material';
import { LogOut } from 'lucide-react';
import { useAuth } from '@/features/auth/model/authStore';
import { resetAuth } from '@/features/auth/model/authService';

interface AppTopBarProps {
  drawerWidth: number;
}

export default function AppTopBar({ drawerWidth }: AppTopBarProps) {
  const { state, dispatch } = useAuth();

  const handleLogout = () => {
    resetAuth(dispatch);
    window.location.reload();
  };

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
          <Button
            color="inherit"
            startIcon={<LogOut size={18} />}
            onClick={handleLogout}
            size="small"
          >
            Logout
          </Button>
        </Box>
      </Toolbar>
    </AppBar>
  );
}
