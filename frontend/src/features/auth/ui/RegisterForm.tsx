import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useNavigate, Link as RouterLink } from 'react-router-dom';
import { Box, Stack, TextField, Button, Divider, Link as MuiLink } from '@mui/material';
import { registerSchema, RegisterForm as RegisterFormValues } from '../lib/registerSchema';
import { useRegisterMutation } from '../model/useRegisterMutation';

interface RegisterFormProps {
  /** Вызывается после успешной регистрации (по умолчанию — переход на /dashboard). */
  onSuccess?: () => void;
  /** Колбэк кнопки Back. */
  onBack?: () => void;
}

/**
 * Форма регистрации.
 *
 * RHF + zodResolver (образец — `features/trading/ui/TradeStockDialog.tsx`).
 * После успеха мутации вызывает `onSuccess` (по умолчанию — `navigate('/dashboard')`).
 * Во время запроса кнопка Create Account заблокирована, поля отключены.
 * Ошибки backend (409 на дубль email/username и т.п.) показываются тостом в
 * `onError` хука.
 */
export default function RegisterForm({ onSuccess, onBack }: RegisterFormProps) {
  const navigate = useNavigate();
  const { mutate, isPending } = useRegisterMutation();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: { username: '', email: '', password: '', confirmPassword: '' },
  });

  const onSubmit = (values: RegisterFormValues) => {
    mutate(
      {
        username: values.username,
        email: values.email,
        password: values.password,
      },
      {
        onSuccess: () => {
          (onSuccess ?? (() => navigate('/dashboard')))();
        },
      }
    );
  };

  const handleBack = () => {
    (onBack ?? (() => navigate('/dashboard')))();
  };

  return (
    <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
      <Stack spacing={2}>
        <TextField
          label="Username"
          autoComplete="username"
          autoFocus
          fullWidth
          required
          disabled={isPending}
          error={!!errors.username}
          helperText={errors.username?.message}
          {...register('username')}
        />
        <TextField
          label="Email"
          type="email"
          autoComplete="email"
          fullWidth
          required
          disabled={isPending}
          error={!!errors.email}
          helperText={errors.email?.message}
          {...register('email')}
        />
        <TextField
          label="Password"
          type="password"
          autoComplete="new-password"
          fullWidth
          required
          disabled={isPending}
          error={!!errors.password}
          helperText={errors.password?.message}
          {...register('password')}
        />
        <TextField
          label="Confirm Password"
          type="password"
          autoComplete="new-password"
          fullWidth
          required
          disabled={isPending}
          error={!!errors.confirmPassword}
          helperText={errors.confirmPassword?.message}
          {...register('confirmPassword')}
        />
        <Button
          type="submit"
          variant="contained"
          size="large"
          fullWidth
          loading={isPending}
        >
          Create Account
        </Button>
        <Button onClick={handleBack} size="large" fullWidth disabled={isPending}>
          Back
        </Button>
        <Divider />
        <Box sx={{ textAlign: 'center' }}>
          <MuiLink component={RouterLink} to="/login" underline="hover">
            Already have an account? Sign in
          </MuiLink>
        </Box>
      </Stack>
    </Box>
  );
}
