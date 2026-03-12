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
      <Paper p="xl" shadow="sm" withBorder w={360}>
        <Title order={2} mb="md">Sign in</Title>
        <form onSubmit={handleSubmit}>
          <Stack gap="md">
            <TextInput
              label="Email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              placeholder="you@example.com"
            />
            <PasswordInput
              label="Password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              placeholder="••••••••"
            />
            {error && <Text c="red" size="sm">{error}</Text>}
            <Button type="submit" loading={loading} fullWidth>Sign in</Button>
            <Text size="sm" c="dimmed">
              Don't have an account? <Link to="/register">Register</Link>
            </Text>
          </Stack>
        </form>
      </Paper>
    </Stack>
  )
}
