import { Container, Typography, Box } from '@mui/material';

export default function RegisterPage() {
  return (
    <Container maxWidth="sm">
      <Box sx={{ mt: 8, textAlign: 'center' }}>
        <Typography variant="h4" component="h1" gutterBottom>
          Register
        </Typography>
      </Box>
    </Container>
  );
}
