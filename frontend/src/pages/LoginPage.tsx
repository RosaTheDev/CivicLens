import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { TextInput, PasswordInput, Button, Stack, Text, Paper, Title } from '@mantine/core'
import { useAuth } from '../context/AuthContext'

const API_BASE = '/api'

export default function LoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const { login } = useAuth()
  const navigate = useNavigate()

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const res = await fetch(`${API_BASE}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password }),
      })
      const data = await res.json()
      if (!res.ok) throw new Error(data.error || 'Login failed')
      login(data.token, {
        userId: data.userId,
        email: data.email,
        displayName: data.displayName,
      })
      navigate('/', { replace: true })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Stack align="center" justify="center" style={{ minHeight: '80vh' }}>
      <div className="civic-auth-hero">
        <Text fw={700} className="civic-heading" size="lg">Welcome back to CivicLens</Text>
        <Text size="sm" c="dimmed">Track your representatives, compare their priorities, and keep your civic notes in one place.</Text>
      </div>
      <Paper p="xl" shadow="sm" withBorder w={380} className="civic-glass-card civic-auth-card">
        <Title order={2} mb="md" className="civic-heading">Sign in</Title>
        <Text size="sm" className="civic-auth-helper" mb="md">
          Use the email and password you registered with.
        </Text>
        <form onSubmit={handleSubmit} className="civic-auth-form">
          <Stack gap="md">
            <TextInput
              label="Email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              placeholder="you@example.com"
              autoComplete="email"
            />
            <PasswordInput
              label="Password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              placeholder="••••••••"
              autoComplete="current-password"
            />
            {error && <Text c="red" size="sm" className="civic-error-text" role="alert">{error}</Text>}
            <Button type="submit" loading={loading} fullWidth className="civic-action-button">Sign in</Button>
            <Text size="sm" c="dimmed">
              Don't have an account? <Link className="civic-auth-link" to="/register">Register</Link>
            </Text>
          </Stack>
        </form>
      </Paper>
    </Stack>
  )
}
