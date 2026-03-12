import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { TextInput, PasswordInput, Button, Stack, Text, Paper, Title } from '@mantine/core'
import { useAuth } from '../context/AuthContext'

const API_BASE = '/api'

export default function RegisterPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const { login } = useAuth()
  const navigate = useNavigate()

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const res = await fetch(`${API_BASE}/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password, displayName: displayName || undefined }),
      })
      const data = await res.json()
      if (!res.ok) throw new Error(data.error || 'Registration failed')
      login(data.token, {
        userId: data.userId,
        email: data.email,
        displayName: data.displayName,
      })
      navigate('/', { replace: true })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Registration failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Stack align="center" justify="center" style={{ minHeight: '80vh' }}>
      <Paper p="xl" shadow="sm" withBorder w={360}>
        <Title order={2} mb="md">Create account</Title>
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
            <TextInput
              label="Display name"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              placeholder="Optional"
            />
            <PasswordInput
              label="Password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              placeholder="At least 8 characters"
            />
            {error && <Text c="red" size="sm">{error}</Text>}
            <Button type="submit" loading={loading} fullWidth>Register</Button>
            <Text size="sm" c="dimmed">
              Already have an account? <Link to="/login">Sign in</Link>
            </Text>
          </Stack>
        </form>
      </Paper>
    </Stack>
  )
}
