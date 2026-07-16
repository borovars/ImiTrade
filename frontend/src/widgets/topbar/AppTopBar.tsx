import { AppBar, Toolbar, Typography, Box, Tooltip, Skeleton } from '@mui/material';
import { Info } from 'lucide-react';
import { NavLink } from 'react-router-dom';
import { IconButton } from '@mui/material';
import { useLogout } from '@/features/auth/model/useLogout';
import { useAccountQuery } from '@/features/account/model/useAccountQuery';
import { formatMoney } from '@/shared/utils/format';
import DesktopNav from './DesktopNav';
import MobileNav from './MobileNav';
import UserMenu from './UserMenu';

/**
 * Верхняя панель приложения (адаптивная).
 *
 * Layout:
 * - слева — бургер `MobileNav` (только xs/sm) + бренд «ImiTrade» + иконка About;
 * - по центру — `DesktopNav` (только md+), абсолютно отцентрована;
 * - справа — блок «Активы» (баланс + портфель) + `UserMenu` (меню аккаунта/гостя).
 *
 * Переключение desktop/mobile — через MUI breakpoints (`sx.display: { xs, md }`),
 * без `useMediaQuery`. Auth-логика не здесь: `useLogout` инкапсулирует выход,
 * `UserMenu` только отображает состояние и вызывает колбэк.
 */
export default function AppTopBar() {
  const logout = useLogout();
  const { data: account, isLoading: accountLoading } = useAccountQuery();

  // totalAssets = balance + portfolioValue. В DTO этого поля нет — считаем на
  // фронте из двух примитивов backend. Показываем всегда: эндпоинт /account
  // доступен и гостю (X-Guest-Token), сводка едина для всех режимов.
  const totalAssets = account ? account.balance + account.portfolioValue : null;

  return (
    <AppBar position="fixed">
      <Toolbar sx={{ position: 'relative' }}>
        {/* Слева: бургер (mobile) + бренд + About. */}
        <MobileNav />
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <Typography variant="h6" noWrap component="div">
            ImiTrade
          </Typography>
          <IconButton
            component={NavLink}
            to="/about"
            color="inherit"
            size="small"
            aria-label="О проекте"
            sx={{
              opacity: 0.7,
              '&.active': { opacity: 1, bgcolor: 'rgba(255, 255, 255, 0.15)' },
            }}
          >
            <Info size={18} />
          </IconButton>
        </Box>

        {/* Центр: desktop-навигация (md+). */}
        <DesktopNav />

        {/* Справа: Активы + меню пользователя. */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, ml: 'auto' }}>
          {accountLoading ? (
            <Skeleton variant="rounded" width={120} height={32} />
          ) : (
            totalAssets !== null && (
              <Tooltip title="Баланс + стоимость портфеля" arrow>
                <Box
                  sx={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 0.75,
                    px: 1.5,
                    py: 0.5,
                    borderRadius: 1,
                    bgcolor: 'rgba(255, 255, 255, 0.15)',
                  }}
                >
                  <Typography variant="caption" sx={{ display: { xs: 'none', sm: 'inline' }, opacity: 0.8 }}>
                    Активы:
                  </Typography>
                  <Typography variant="body2" sx={{ fontWeight: 700 }} noWrap>
                    {formatMoney(totalAssets)}
                  </Typography>
                </Box>
              </Tooltip>
            )
          )}

          <UserMenu onLogout={logout} />
        </Box>
      </Toolbar>
    </AppBar>
  );
}
