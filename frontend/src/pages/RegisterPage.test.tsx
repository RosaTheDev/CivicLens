import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import { MantineProvider } from '@mantine/core'
import RegisterPage from './RegisterPage'
import { AuthProvider } from '../context/AuthContext'

function renderRegister() {
  return render(
    <MantineProvider>
      <BrowserRouter>
        <AuthProvider>
          <RegisterPage />
        </AuthProvider>
      </BrowserRouter>
    </MantineProvider>,
  )
}

describe('RegisterPage', () => {
  it('renders email, display name, password and submit button', () => {
    renderRegister()
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/display name/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /register/i })).toBeInTheDocument()
  })

  it('calls fetch on submit with register payload', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => ({ token: 'jwt', userId: 1, email: 'u@test.com', displayName: 'User' }),
    } as Response)
    renderRegister()
    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: 'u@test.com' } })
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'password123' } })
    fireEvent.click(screen.getByRole('button', { name: /register/i }))
    await waitFor(() => {
      expect(fetchSpy).toHaveBeenCalledWith(
        '/api/auth/register',
        expect.objectContaining({
          method: 'POST',
          body: expect.stringContaining('u@test.com'),
        }),
      )
    })
    fetchSpy.mockRestore()
  })
})
