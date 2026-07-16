import { useState } from 'react';
import { IconButton, Drawer, Box, List, ListItemButton, ListItemIcon, ListItemText, Typography } from '@mui/material';
import { Menu } from 'lucide-react';
import { NavLink } from 'react-router-dom';
import { NAV_ITEMS } from './navItems';

/**
 * Мобильная навигация (бургер + левый Drawer) для xs/sm.
 *
 * На md и выше иконка-бургер скрыта (`display: { xs: 'flex', md: 'none' }`) —
 * навигацию берёт на себя `DesktopNav`. Drawer содержит те же пункты, что и
 * desktop (общий `NAV_ITEMS`), клик по пункту закрывает Drawer.
 *
 * Текущий раздел подсвечивается `.active`-классом `NavLink`.
 */
export default function MobileNav() {
  const [open, setOpen] = useState(false);

  const close = () => setOpen(false);

  return (
    <>
      <IconButton
        color="inherit"
        edge="start"
        onClick={() => setOpen(true)}
        aria-label="Открыть меню"
        sx={{ display: { xs: 'flex', md: 'none' }, mr: 0.5 }}
      >
        <Menu size={24} />
      </IconButton>

      <Drawer open={open} onClose={close} PaperProps={{ sx: { width: 270 } }}>
        <Box sx={{ p: 2 }}>
          <Typography variant="h6" component="div" sx={{ fontWeight: 700 }}>
            ImiTrade
          </Typography>
        </Box>
        <List onClick={close}>
          {NAV_ITEMS.map((item) => (
            <ListItemButton
              key={item.path}
              component={NavLink}
              to={item.path}
              end={item.end}
              sx={{
                '&.active': {
                  color: 'primary.main',
                  fontWeight: 700,
                  bgcolor: 'action.selected',
                },
              }}
            >
              <ListItemIcon sx={{ minWidth: 40, color: 'inherit' }}>{item.icon}</ListItemIcon>
              <ListItemText primary={item.label} />
            </ListItemButton>
          ))}
        </List>
      </Drawer>
    </>
  );
}
