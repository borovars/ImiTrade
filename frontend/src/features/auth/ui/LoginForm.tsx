import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useNavigate, Link as RouterLink } from 'react-router-dom';
import { Box, Stack, TextField, Button, Divider, Link as MuiLink } from '@mui/material';
import { loginSchema, LoginForm as LoginFormValues } from '../lib/loginSchema';
import { useLoginMutation } from '../model/useLoginMutation';

interface LoginFormProps {
  /** Вызывается после успешного входа (по умолчанию — переход на /dashboard). */
  onSuccess?: () => void;
  /** Колбэк кнопки Back. */
  onBack?: () => void;
}

/**
 * Форма входа.
 *
 * RHF + zodResolver (образец — `features/trading/ui/TradeStockDialog.tsx`).
 * После успеха мутации вызывает `onSuccess` (по умолчанию — `navigate('/dashboard')`).
 * Во время запроса кнопка Sign In заблокирована (`loading`-проп MUI), поля
 * отключены. Ошибки backend показываются тостом в `onError` хука.
 */
export default function LoginForm({ onSuccess, onBack }: LoginFormProps) {
  const navigate = useNavigate();
  const { mutate, isPending } = useLoginMutation();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  });

  const onSubmit = (values: LoginFormValues) => {
    mutate(values, {
      onSuccess: () => {
        (onSuccess ?? (() => navigate('/dashboard')))();
      },
    });
  };

  const handleBack = () => {
    (onBack ?? (() => navigate('/dashboard')))();
  };

  return (
    <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
      <Stack spacing={2}>
        <TextField
          label="Email"
          type="email"
          autoComplete="email"
          autoFocus
          fullWidth
          required
          disabled={isPending}
          error={!!errors.email}
          helperText={errors.email?.message}
          {...register('email')}
        />
        <TextField
          label="Пароль"
          type="password"
          autoComplete="current-password"
          fullWidth
          required
          disabled={isPending}
          error={!!errors.password}
          helperText={errors.password?.message}
          {...register('password')}
        />
        <Button
          type="submit"
          variant="contained"
          size="large"
          fullWidth
          loading={isPending}
        >
          Войти
        </Button>
        <Button onClick={handleBack} size="large" fullWidth disabled={isPending}>
          Назад
        </Button>
        <Divider />
        <Box sx={{ textAlign: 'center' }}>
          <MuiLink component={RouterLink} to="/register" underline="hover">
            Нет аккаунта? Создать
          </MuiLink>
        </Box>
      </Stack>
    </Box>
  );
}
