import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  Box,
  Stack,
} from '@mui/material';
import { storage } from '@/shared/lib/storage';

/**
 * Приветственное окно онбординга при первом визите.
 *
 * Полностью независимый компонент: сам читает флаг `onboarding_completed`
 * из localStorage через централизованную утилиту `storage` и сам управляет
 * своей видимостью — props не требует, монтируется в `AppLayout`.
 *
 * Показывается только один раз: при первом рендере `open=true`, если флаг
 * ещё не установлен; кнопка «Начать» или переход на /about сохраняют флаг,
 * после чего окно больше не появляется автоматически. Очистка localStorage
 * (например, при выходе из аккаунта через `clearAuth()` не затрагивает этот
 * ключ, но ручная очистка браузера вернёт окно).
 *
 * Цвета завязаны на токены темы (`primary`, `action.selected`, `text.*`),
 * без хардкод-hex — фирменный зелёный теперь живёт в `palette.primary`.
 */
export default function WelcomeDialog() {
  const navigate = useNavigate();
  // Ленивый инициализатор читает storage один раз при первом рендере —
  // SPA без SSR, поэтому флэша скрытого/показанного диалога не возникает.
  const [open, setOpen] = useState(() => !storage.isOnboardingCompleted());

  /** Закрыть диалог и пометить онбординг пройденным. */
  const complete = () => {
    storage.setOnboardingCompleted();
    setOpen(false);
  };

  /** Перейти на страницу About, предварительно завершив онбординг. */
  const goToAbout = () => {
    complete();
    navigate('/about');
  };

  return (
    <Dialog open={open} maxWidth="xs" fullWidth>
      <DialogTitle>
        <Stack direction="row" alignItems="baseline" spacing={1}>
          <Typography variant="h5" component="span" color="primary" sx={{ fontWeight: 700 }}>
            ImiTrade
          </Typography>
        </Stack>
      </DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 0.5 }}>
          <Typography variant="h6" component="p" sx={{ fontWeight: 600 }}>
            Добро пожаловать в ImiTrade
          </Typography>

          <Typography variant="body2" color="text.secondary">
            ImiTrade — это учебный симулятор инвестиций, позволяющий изучать работу
            фондового рынка без риска потерять реальные деньги.
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Все сделки совершаются за виртуальные кредиты, а рыночные данные и цены
            акций поступают с Московской биржи (MOEX).
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Вы можете покупать и продавать акции, отслеживать стоимость своего
            портфеля, анализировать изменение цен и изучать основы инвестирования.
          </Typography>

          <Box
            sx={{
              py: 1.5,
              px: 2,
              bgcolor: 'action.selected',
              borderRadius: 1,
            }}
          >
            <Typography variant="body2" sx={{ fontWeight: 600, mb: 0.5 }}>
              Стартовый капитал
            </Typography>
            <Typography variant="body2" color="text.secondary" component="div">
              <ul style={{ margin: 0, paddingLeft: '1.1rem' }}>
                <li>При первом посещении вы автоматически получаете 5 000 виртуальных кредитов.</li>
                <li>После регистрации вы сохраните свой прогресс и получите бонус +20 000 кредитов.</li>
              </ul>
            </Typography>
          </Box>
        </Stack>
      </DialogContent>
      <DialogActions sx={{ flexDirection: 'column', alignItems: 'stretch', gap: 1, px: 3, pb: 2 }}>
        <Button variant="contained" color="primary" onClick={complete} fullWidth>
          Начать
        </Button>
        <Button color="primary" onClick={goToAbout} size="small" sx={{ textTransform: 'none' }}>
          Подробнее о проекте
        </Button>
      </DialogActions>
    </Dialog>
  );
}
