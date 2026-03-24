import { Routes, Route, Navigate, useLocation } from 'react-router-dom'
import { useAuth } from './context/AuthContext'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import DashboardPage from './pages/DashboardPage'
import RepresentativeDetailPage from './pages/RepresentativeDetailPage'
import WatchlistPage from './pages/WatchlistPage'
import ElectionsPage from './pages/ElectionsPage'
import { AppShell, Group, Text, Button, Stack, Container, Badge } from '@mantine/core'
import { Link } from 'react-router-dom'
import vintageFlag from './assets/vintage-flag.svg'

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { token } = useAuth()
  if (!token) return <Navigate to="/login" replace />
  return <>{children}</>
}

function Layout({ children }: { children: React.ReactNode }) {
  const { token, logout } = useAuth()
  const location = useLocation()
  const showGlobalPill = location.pathname !== '/elections'
  return (
    <AppShell header={{ height: 86 }} padding="md" className="civic-app-shell">
      <AppShell.Header className="civic-header">
        <div className="civic-header-inner">
          <div className="civic-header-left">
            <Text fw={700} size="lg" className="civic-wordmark" component={Link} to="/" style={{ color: 'inherit', textDecoration: 'none' }}>
              CivicLens
            </Text>
          </div>
          <div className="civic-header-center">
            <Link to="/" className="civic-header-flag-link" aria-label="Go to CivicLens home">
              <img src={vintageFlag} alt="" className="civic-header-flag" />
            </Link>
          </div>
          <div className="civic-header-right">
            {token ? (
              <Group>
                <Button component={Link} to="/" variant="default" size="md" radius="xl" className="civic-nav-button civic-header-button">Dashboard</Button>
                <Button component={Link} to="/elections" variant="default" size="md" radius="xl" className="civic-nav-button civic-header-button">Elections</Button>
                <Button component={Link} to="/watchlist" variant="default" size="md" radius="xl" className="civic-nav-button civic-header-button">My Watchlist</Button>
                <Button variant="filled" size="md" radius="xl" className="civic-nav-button civic-nav-button--primary civic-header-button" onClick={logout}>Logout</Button>
              </Group>
            ) : (
              <Group>
                <Button component={Link} to="/login" variant="subtle" size="md" className="civic-nav-button civic-header-button">Login</Button>
                <Button component={Link} to="/register" size="md" className="civic-nav-button civic-nav-button--primary civic-header-button">Register</Button>
              </Group>
            )}
          </div>
        </div>
      </AppShell.Header>
      <AppShell.Main className="civic-main">
        <div className="civic-ambient" aria-hidden="true">
          <div className="civic-orb civic-orb--one" />
          <div className="civic-orb civic-orb--two" />
          <div className="civic-orb civic-orb--three" />
        </div>
        <Container size="lg" py="md" className="civic-content">
          <Stack gap="md">
            {showGlobalPill ? (
              <Badge className="civic-pill" variant="light" w="fit-content">Civic transparency made simple</Badge>
            ) : null}
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
      <Route path="/elections" element={<Layout><ProtectedRoute><ElectionsPage /></ProtectedRoute></Layout>} />
      <Route path="/representatives/:id" element={<Layout><ProtectedRoute><RepresentativeDetailPage /></ProtectedRoute></Layout>} />
      <Route path="/watchlist" element={<Layout><ProtectedRoute><WatchlistPage /></ProtectedRoute></Layout>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
