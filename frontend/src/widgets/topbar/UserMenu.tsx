import { useState, MouseEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, Menu, MenuItem, Avatar, Box, Typography } from '@mui/material';
import { User as UserIcon, ChevronDown, UserPlus, LogIn, LogOut } from 'lucide-react';
import { useAuth } from '@/features/auth/model/authStore';
import { CURRENCY_SYMBOL } from '@/shared/utils/format';

interface UserMenuProps {
  /** Выход из аккаунта. Реализация инкапсулирована в существующем `useLogout`. */
  onLogout: () => void;
}

/**
 * Единое меню пользователя (замена разрозненным кнопкам Login/Register/Logout).
 *
 * Чисто UI-компонент: читает `useAuth().state` только для подписи триггера,
 * навигация — через `useNavigate` (те же маршруты /login, /register), выход —
 * через колбэк `onLogout` от родителя. Сама auth-логика здесь не живёт.
 *
 * - Guest: триггер «Гость» → меню с «Регистрация (+20 000)» и «Вход».
 * - Auth: триггер с username (или «Аккаунт») → меню с «Выйти».
 */
export default function UserMenu({ onLogout }: UserMenuProps) {
  const { state } = useAuth();
  const navigate = useNavigate();
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);
  const open = Boolean(anchorEl);

  const isAuth = state.userType === 'auth';
  const label = isAuth ? state.user?.username || 'Аккаунт' : 'Гость';

  const openMenu = (e: MouseEvent<HTMLElement>) => setAnchorEl(e.currentTarget);
  const closeMenu = () => setAnchorEl(null);

  const go = (path: string) => {
    closeMenu();
    navigate(path);
  };

  const handleLogout = () => {
    closeMenu();
    onLogout();
  };

  return (
    <>
      <Button color="inherit" onClick={openMenu} aria-label="Меню пользователя" sx={{ textTransform: 'none' }}>
        <Avatar sx={{ width: 28, height: 28, bgcolor: 'rgba(255, 255, 255, 0.25)', mr: { xs: 0, sm: 1 } }}>
          <UserIcon size={18} />
        </Avatar>
        <Box component="span" sx={{ display: { xs: 'none', sm: 'inline' } }}>
          <Typography component="span" sx={{ fontWeight: 600, maxWidth: 140 }} noWrap>
            {label}
          </Typography>
          <ChevronDown size={16} style={{ verticalAlign: 'middle', marginLeft: 4 }} />
        </Box>
      </Button>

      <Menu anchorEl={anchorEl} open={open} onClose={closeMenu} keepMounted>
        {isAuth ? (
          <MenuItem onClick={handleLogout}>
            <LogOut size={18} style={{ marginRight: 8 }} />
            Выйти
          </MenuItem>
        ) : (
          [
            <MenuItem key="register" onClick={() => go('/register')}>
              <UserPlus size={18} style={{ marginRight: 8 }} />
              Регистрация (+20 000 {CURRENCY_SYMBOL})
            </MenuItem>,
            <MenuItem key="login" onClick={() => go('/login')}>
              <LogIn size={18} style={{ marginRight: 8 }} />
              Вход
            </MenuItem>,
          ]
        )}
      </Menu>
    </>
  );
}
