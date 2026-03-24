import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './context/AuthContext'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import DashboardPage from './pages/DashboardPage'
import RepresentativeDetailPage from './pages/RepresentativeDetailPage'
import WatchlistPage from './pages/WatchlistPage'
import { AppShell, Group, Text, Button, Stack, Container, Badge } from '@mantine/core'
import { Link } from 'react-router-dom'

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { token } = useAuth()
  if (!token) return <Navigate to="/login" replace />
  return <>{children}</>
}

function Layout({ children }: { children: React.ReactNode }) {
  const { token, logout } = useAuth()
  return (
    <AppShell header={{ height: 64 }} padding="md">
      <AppShell.Header
        style={{
          borderBottom: '1px solid var(--mantine-color-gray-3)',
          background:
            'linear-gradient(90deg, color-mix(in srgb, var(--mantine-color-blue-0) 55%, white), color-mix(in srgb, var(--mantine-color-cyan-0) 35%, white))',
        }}
      >
        <Group h="100%" px="md" justify="space-between">
          <Text fw={700} size="lg" component={Link} to="/" style={{ color: 'inherit', textDecoration: 'none' }}>
            CivicLens
          </Text>
          {token ? (
            <Group>
              <Button component={Link} to="/" variant="default" size="sm">Dashboard</Button>
              <Button component={Link} to="/watchlist" variant="default" size="sm">My Watchlist</Button>
              <Button variant="filled" size="sm" color="gray" onClick={logout}>Logout</Button>
            </Group>
          ) : (
            <Group>
              <Button component={Link} to="/login" variant="subtle" size="sm">Login</Button>
              <Button component={Link} to="/register" size="sm">Register</Button>
            </Group>
          )}
        </Group>
      </AppShell.Header>
      <AppShell.Main>
        <Container size="lg" py="md">
          <Stack gap="md">
            <Badge variant="light" color="blue" w="fit-content">Civic transparency made simple</Badge>
            {children}
          </Stack>
        </Container>
      </AppShell.Main>
    </AppShell>
  )
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/" element={<Layout><ProtectedRoute><DashboardPage /></ProtectedRoute></Layout>} />
      <Route path="/representatives/:id" element={<Layout><ProtectedRoute><RepresentativeDetailPage /></ProtectedRoute></Layout>} />
      <Route path="/watchlist" element={<Layout><ProtectedRoute><WatchlistPage /></ProtectedRoute></Layout>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
