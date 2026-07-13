import { Container, Typography, Box, Paper } from '@mui/material';
import { Navigate } from 'react-router-dom';
import RegisterForm from '@/features/auth/ui/RegisterForm';
import { useAuthState } from '@/features/auth/model/authStore';

/**
 * Страница регистрации.
 *
 * Тонкая обёртка над `RegisterForm`. Если посетитель уже залогинен — редирект
 * на `/dashboard`.
 */
export default function RegisterPage() {
  const { userType } = useAuthState();

  if (userType === 'auth') {
    return <Navigate to="/dashboard" replace />;
  }

  return (
    <Container maxWidth="xs">
      <Box sx={{ mt: 8, mb: 4, textAlign: 'center' }}>
        <Typography variant="h4" component="h1" gutterBottom>
          Создать аккаунт
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Сохраните прогресс и продолжите торговать
        </Typography>
      </Box>
      <Paper sx={{ p: 3 }}>
        <RegisterForm />
      </Paper>
    </Container>
  );
}
