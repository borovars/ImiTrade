import { Box, Toolbar } from '@mui/material';
import { Outlet } from 'react-router-dom';
import AppSidebar from '@/widgets/sidebar/AppSidebar';
import AppTopBar from '@/widgets/topbar/AppTopBar';

const DRAWER_WIDTH = 240;

export default function AppLayout() {
  return (
    <Box sx={{ display: 'flex' }}>
      <AppTopBar drawerWidth={DRAWER_WIDTH} />
      <AppSidebar drawerWidth={DRAWER_WIDTH} />
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: 3,
          width: { sm: `calc(100% - ${DRAWER_WIDTH}px)` },
        }}
      >
        <Toolbar />
        <Outlet />
      </Box>
    </Box>
  );
}
