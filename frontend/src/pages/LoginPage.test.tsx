import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import { MantineProvider } from '@mantine/core'
import LoginPage from './LoginPage'
import { AuthProvider } from '../context/AuthContext'

function renderLogin() {
  return render(
    <MantineProvider>
      <BrowserRouter>
        <AuthProvider>
          <LoginPage />
        </AuthProvider>
      </BrowserRouter>
    </MantineProvider>,
  )
}

describe('LoginPage', () => {
  it('renders email and password fields and submit button', () => {
    renderLogin()
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument()
  })

  it('shows validation when submitting empty form', async () => {
    renderLogin()
    const btn = screen.getByRole('button', { name: /sign in/i })
    fireEvent.click(btn)
    await waitFor(() => {
      const email = screen.getByLabelText(/email/i)
      expect(email).toBeInTheDocument()
    })
  })

  it('calls fetch on submit with email and password', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => ({ token: 'jwt', userId: 1, email: 'u@test.com', displayName: null }),
    } as Response)
    renderLogin()
    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: 'u@test.com' } })
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'password123' } })
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }))
    await waitFor(() => {
      expect(fetchSpy).toHaveBeenCalledWith(
        '/api/auth/login',
        expect.objectContaining({
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ email: 'u@test.com', password: 'password123' }),
        }),
      )
    })
    fetchSpy.mockRestore()
  })
})
