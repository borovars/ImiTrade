import { Container, Typography, Box, Card, CardContent, List, ListItem, ListItemIcon, ListItemText } from '@mui/material';
import { CheckCircle2 } from 'lucide-react';

/**
 * Страница «О проекте» (маршрут /about).
 *
 * Статичный контент: описание ImiTrade, возможности, источник данных, стек
 * технологий, архитектура, назначение. Верстка адаптивная — карточки в
 * отзывчивой сетке MUI Grid, на мобильных одна колонка, на десктопе до двух.
 * Текст разбит на отдельные Card, без длинных неструктурированных блоков.
 *
 * Цвета завязаны на токены темы (primary, text.*); фирменный зелёный живёт
 * в palette.primary.
 */
export default function AboutPage() {
  return (
    <Container maxWidth="lg">
      <Box sx={{ mt: 4, mb: 4 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          О проекте{' '}
          <Typography component="span" color="primary" sx={{ fontWeight: 700 }}>
            ImiTrade
          </Typography>
        </Typography>
      </Box>

      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' },
          gap: 2,
        }}
      >
        {/* Что такое ImiTrade */}
        <Card>
          <CardContent>
            <Typography variant="h6" component="h2" gutterBottom sx={{ fontWeight: 600 }}>
              Что такое ImiTrade
            </Typography>
            <Typography variant="body2" color="text.secondary" paragraph>
              ImiTrade — учебный инвестиционный симулятор, созданный для изучения
              принципов работы фондового рынка.
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Проект позволяет безопасно знакомиться с покупкой и продажей акций,
              не используя реальные деньги.
            </Typography>
          </CardContent>
        </Card>

        {/* Возможности */}
        <Card>
          <CardContent>
            <Typography variant="h6" component="h2" gutterBottom sx={{ fontWeight: 600 }}>
              Возможности
            </Typography>
            <List dense disablePadding>
              {FEATURES.map((feature) => (
                <ListItem key={feature} disableGutters>
                  <ListItemIcon sx={{ minWidth: 32, color: 'primary.main' }}>
                    <CheckCircle2 size={18} />
                  </ListItemIcon>
                  <ListItemText primary={feature} primaryTypographyProps={{ variant: 'body2' }} />
                </ListItem>
              ))}
            </List>
          </CardContent>
        </Card>

        {/* Откуда берутся данные */}
        <Card>
          <CardContent>
            <Typography variant="h6" component="h2" gutterBottom sx={{ fontWeight: 600 }}>
              Откуда берутся данные
            </Typography>
            <Typography variant="body2" color="text.secondary" paragraph>
              Рыночные данные поступают через MOEX ISS API.
            </Typography>
            <Typography variant="body2" color="text.secondary" paragraph>
              Цены и исторические данные соответствуют информации Московской биржи.
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Все денежные средства в приложении являются виртуальными.
            </Typography>
          </CardContent>
        </Card>

        {/* Назначение проекта */}
        <Card>
          <CardContent>
            <Typography variant="h6" component="h2" gutterBottom sx={{ fontWeight: 600 }}>
              Назначение проекта
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Проект создан как pet-проект для демонстрации навыков разработки
              современных веб-приложений и изучения архитектуры инвестиционных
              сервисов.
            </Typography>
          </CardContent>
        </Card>

        {/* Используемые технологии */}
        <Card sx={{ gridColumn: { xs: '1fr', md: '1 / -1' } }}>
          <CardContent>
            <Typography variant="h6" component="h2" gutterBottom sx={{ fontWeight: 600 }}>
              Используемые технологии
            </Typography>
            <Box
              sx={{
                display: 'grid',
                gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', md: '1fr 1fr 1fr' },
                gap: 2,
                mt: 1,
              }}
            >
              <TechGroup title="Backend" items={TECH_BACKEND} />
              <TechGroup title="Frontend" items={TECH_FRONTEND} />
              <TechGroup title="Интеграции" items={TECH_INTEGRATIONS} />
            </Box>
          </CardContent>
        </Card>

        {/* Архитектура */}
        <Card sx={{ gridColumn: { xs: '1fr', md: '1 / -1' } }}>
          <CardContent>
            <Typography variant="h6" component="h2" gutterBottom sx={{ fontWeight: 600 }}>
              Архитектура
            </Typography>
            <Typography variant="body2" color="text.secondary" paragraph>
              Backend реализован по feature-based архитектуре.
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Frontend также использует feature-based структуру с разделением на
              shared, entities, features, widgets и pages.
            </Typography>
          </CardContent>
        </Card>
      </Box>
    </Container>
  );
}

/** Колонка технологий с заголовком и списком. */
function TechGroup({ title, items }: { title: string; items: string[] }) {
  return (
    <Box>
      <Typography variant="subtitle2" color="primary" sx={{ fontWeight: 700, mb: 0.5 }}>
        {title}
      </Typography>
      <List dense disablePadding>
        {items.map((item) => (
          <ListItem key={item} disableGutters>
            <ListItemText primary={item} primaryTypographyProps={{ variant: 'body2', color: 'text.secondary' }} />
          </ListItem>
        ))}
      </List>
    </Box>
  );
}

const FEATURES: string[] = [
  'Покупка и продажа акций',
  'Виртуальный инвестиционный счёт',
  'Просмотр портфеля',
  'История операций',
  'Просмотр информации о компаниях',
  'Графики изменения стоимости акций',
  'Гостевой режим',
  'Регистрация с сохранением прогресса',
];

const TECH_BACKEND: string[] = [
  'Java 25',
  'Spring Boot',
  'Spring Security',
  'Spring Data JPA',
  'PostgreSQL',
  'Flyway',
  'JWT',
  'OpenAPI',
];

const TECH_FRONTEND: string[] = [
  'React',
  'TypeScript',
  'Vite',
  'Material UI',
  'TanStack Query',
  'React Router',
  'Lightweight Charts',
];

const TECH_INTEGRATIONS: string[] = ['MOEX ISS API'];
