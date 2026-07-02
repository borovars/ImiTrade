import { Container, Typography, Box } from '@mui/material';

export default function PortfolioPage() {
  return (
    <Container maxWidth="lg">
      <Box sx={{ mt: 4 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          Portfolio
        </Typography>
      </Box>
    </Container>
  );
}
