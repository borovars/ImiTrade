import { Container, Typography, Box } from '@mui/material';

export default function AccountPage() {
  return (
    <Container maxWidth="lg">
      <Box sx={{ mt: 4 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          Account
        </Typography>
      </Box>
    </Container>
  );
}
