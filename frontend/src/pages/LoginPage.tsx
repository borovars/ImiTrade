import { Container, Typography, Box, Paper } from '@mui/material';
import { Navigate } from 'react-router-dom';
import LoginForm from '@/features/auth/ui/LoginForm';
import { useAuthState } from '@/features/auth/model/authStore';

/**
 * Страница входа.
 *
 * Тонкая обёртка над `LoginForm`. Если посетитель уже залогинен — редирект на
 * `/dashboard` (не показываем форму входа дважды).
 */
export default function LoginPage() {
  const { userType } = useAuthState();

  if (userType === 'auth') {
    return <Navigate to="/dashboard" replace />;
  }

  return (
    <Container maxWidth="xs">
      <Box sx={{ mt: 8, mb: 4, textAlign: 'center' }}>
        <Typography variant="h4" component="h1" gutterBottom>
          Вход
        </Typography>
        <Typography variant="body2" color="text.secondary">
          С возвращением в ImiTrade
        </Typography>
      </Box>
      <Paper sx={{ p: 3 }}>
        <LoginForm />
      </Paper>
    </Container>
  );
}
