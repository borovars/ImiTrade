import { Box, Toolbar } from '@mui/material';
import { Outlet } from 'react-router-dom';
import AppTopBar from '@/widgets/topbar/AppTopBar';
import WelcomeDialog from '@/features/onboarding/ui/WelcomeDialog';

/**
 * Layout защищённых страниц: фиксированный `AppBar` сверху (на всю ширину —
 * сайдбара нет, навигация живёт в топбаре) и основной контент под ним.
 * `<Toolbar/>` резервирует место под фиксированный AppBar.
 *
 * `WelcomeDialog` монтируется здесь, а не в конкретной странице: онбординг
 * показывается один раз при первом визите на любую страницу layout-группы
 * (редирект `/` → `/dashboard` сюда и попадает). Сам управляет видимостью
 * через localStorage-флаг, props не требует.
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
      <WelcomeDialog />
    </Box>
  );
}
