import { Box, Toolbar } from '@mui/material';
import { Outlet } from 'react-router-dom';
import AppTopBar from '@/widgets/topbar/AppTopBar';

/**
 * Layout защищённых страниц: фиксированный `AppBar` сверху (на всю ширину —
 * сайдбара нет, навигация живёт в топбаре) и основной контент под ним.
 * `<Toolbar/>` резервирует место под фиксированный AppBar.
 */
export default function AppLayout() {
  return (
    <Box sx={{ display: 'flex' }}>
      <AppTopBar />
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: 3,
          width: '100%',
        }}
      >
        <Toolbar />
        <Outlet />
      </Box>
    </Box>
  );
}
