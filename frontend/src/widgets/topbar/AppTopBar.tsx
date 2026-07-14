import { AppBar, Toolbar, Typography, Box, Button, Chip, Skeleton, Tooltip, IconButton } from '@mui/material';
import { LogIn, UserPlus, LogOut, LayoutDashboard, TrendingUp, Wallet, Receipt, Info } from 'lucide-react';
import { Link, NavLink } from 'react-router-dom';
import { useAuth } from '@/features/auth/model/authStore';
import { useLogout } from '@/features/auth/model/useLogout';
import { useAccountQuery } from '@/features/account/model/useAccountQuery';
import { formatMoney } from '@/shared/utils/format';
import type { ReactNode } from 'react';

/**
 * Верхняя панель приложения.
 *
 * Layout: бренд «ImiTrade» слева, основная навигация (Главная / Акции / Портфель /
 * Операции) по центру (абсолютное центрирование относительно toolbar'а), блок
 * аутентификации справа. Перед блоком аутентификации — краткая сводка по аккаунту:
 * суммарная стоимость активов (баланс + стоимость портфеля), обновляется через
 * `useAccountQuery` (тот же query-ключ, что и на Dashboard — запрос дедуплицируется).
 *
 * Текущий раздел подсвечивается (`active`-класс `NavLink`). Боковой сайдбар убран —
 * вся навигация живёт здесь, панель на всю ширину.
 *
 * Logout выполняется без перезагрузки страницы (`useLogout`): очищает storage,
 * React Query cache и переводит приложение в гостевой режим.
 */

interface NavItem {
  label: string;
  path: string;
  icon: ReactNode;
  /** `end` нужен для `/dashboard`, чтобы он не ловил все маршруты (prefix-match). */
  end?: boolean;
}

const NAV_ITEMS: NavItem[] = [
  { label: 'Главная', path: '/dashboard', icon: <LayoutDashboard size={20} />, end: true },
  { label: 'Акции', path: '/stocks', icon: <TrendingUp size={20} /> },
  { label: 'Портфель', path: '/portfolio', icon: <Wallet size={20} /> },
  { label: 'Операции', path: '/transactions', icon: <Receipt size={20} /> },
];

export default function AppTopBar() {
  const { state } = useAuth();
  const logout = useLogout();
  const { data: account, isLoading: accountLoading } = useAccountQuery();

  // totalAssets = balance + portfolioValue. В DTO этого поля нет — считаем на
  // фронте из двух примитивов backend. Показываем всегда: эндпоинт /account
  // доступен и гостю (X-Guest-Token), сводка едина для всех режимов.
  const totalAssets = account ? account.balance + account.portfolioValue : null;

  return (
    <AppBar position="fixed">
      <Toolbar sx={{ position: 'relative' }}>
        {/* Бренд слева + кнопка «О проекте» (только иконка i в кружочке). */}
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

        {/* Навигация по центру (абсолютное центрирование относительно toolbar'а).
            Текущий раздел подсвечивается через .active NavLink. */}
        <Box
          sx={{
            position: 'absolute',
            left: '50%',
            transform: 'translateX(-50%)',
            display: 'flex',
            alignItems: 'center',
            gap: 0.5,
          }}
        >
          {NAV_ITEMS.map((item) => (
            <Button
              key={item.path}
              component={NavLink}
              to={item.path}
              end={item.end}
              color="inherit"
              startIcon={item.icon}
              sx={{
                fontSize: '1.05rem',
                textTransform: 'none',
                opacity: 0.75,
                '&.active': {
                  opacity: 1,
                  fontWeight: 700,
                  bgcolor: 'rgba(255, 255, 255, 0.15)',
                },
              }}
            >
              {item.label}
            </Button>
          ))}
        </Box>

        {/* Блок аутентификации справа. */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, ml: 'auto' }}>
          {/* Краткая сводка по аккаунту: суммарная стоимость (баланс + портфель). */}
          {accountLoading ? (
            <Skeleton variant="rounded" width={150} height={32} />
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
                  <Typography variant="caption" sx={{ opacity: 0.8 }}>
                    Активы:
                  </Typography>
                  <Typography variant="body2" sx={{ fontWeight: 700 }} noWrap>
                    {formatMoney(totalAssets)}
                  </Typography>
                </Box>
              </Tooltip>
            )
          )}

          <Chip
            label={state.userType === 'auth' ? 'Пользователь' : 'Гость'}
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
                sx={{ textTransform: 'none' }}
              >
                Выйти
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
                sx={{ textTransform: 'none' }}
              >
                Войти
              </Button>
              <Button
                color="inherit"
                component={Link}
                to="/register"
                startIcon={<UserPlus size={18} />}
                size="small"
                sx={{ textTransform: 'none' }}
              >
                Регистрация
              </Button>
            </>
          )}
        </Box>
      </Toolbar>
    </AppBar>
  );
}
