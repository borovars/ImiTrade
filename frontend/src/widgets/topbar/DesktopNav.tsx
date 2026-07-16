import { Box, Button } from '@mui/material';
import { NavLink } from 'react-router-dom';
import { NAV_ITEMS } from './navItems';

/**
 * Центральная навигация для десктопа (md и выше).
 *
 * Абсолютно отцентрована относительно toolbar'а (`position: absolute,
 * left: 50%`). На xs/sm скрыта через `display: { xs: 'none', md: 'flex' }` —
 * на мобильных навигация открывается через `MobileNav` (бургер + Drawer).
 *
 * Текущий раздел подсвечивается `.active`-классом `NavLink`.
 */
export default function DesktopNav() {
  return (
    <Box
      sx={{
        position: 'absolute',
        left: '50%',
        transform: 'translateX(-50%)',
        display: { xs: 'none', md: 'flex' },
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
  );
}
