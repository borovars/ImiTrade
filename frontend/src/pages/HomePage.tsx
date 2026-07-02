import { Container, Typography, Box } from '@mui/material';
import { CheckCircle } from 'lucide-react';

export default function HomePage() {
  return (
    <Container maxWidth="sm" sx={{ mt: 8, textAlign: 'center' }}>
      <Typography variant="h3" component="h1" gutterBottom>
        ImiTrade Frontend
      </Typography>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 1, mt: 2 }}>
        <CheckCircle size={24} color="green" />
        <Typography variant="h6" color="text.secondary">
          Frontend initialized successfully
        </Typography>
      </Box>
    </Container>
  );
}
