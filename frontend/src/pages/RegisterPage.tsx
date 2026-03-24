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
      <div className="civic-auth-hero">
        <Text fw={700} className="civic-heading" size="lg">Join CivicLens</Text>
        <Text size="sm" c="dimmed">Build a personal civic record with representative profiles, bill tracking, and your own stance notes.</Text>
      </div>
      <Paper p="xl" shadow="sm" withBorder w={380} className="civic-glass-card civic-auth-card">
        <Title order={2} mb="md" className="civic-heading">Create account</Title>
        <Text size="sm" className="civic-auth-helper" mb="md">
          Create a secure account to save your civic preferences.
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
            <TextInput
              label="Display name"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              placeholder="Optional"
              autoComplete="nickname"
            />
            <PasswordInput
              label="Password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              placeholder="At least 8 characters"
              autoComplete="new-password"
              description="Use at least 8 characters."
            />
            {error && <Text c="red" size="sm" className="civic-error-text" role="alert">{error}</Text>}
            <Button type="submit" loading={loading} fullWidth className="civic-action-button">Register</Button>
            <Text size="sm" c="dimmed">
              Already have an account? <Link className="civic-auth-link" to="/login">Sign in</Link>
            </Text>
          </Stack>
        </form>
      </Paper>
    </Stack>
  )
}
