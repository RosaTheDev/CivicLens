import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen, act } from '@testing-library/react'
import { AuthProvider, useAuth } from '../context/AuthContext'

function Consumer() {
  const { token, user, login, logout } = useAuth()
  return (
    <div>
      <span data-testid="token">{token ?? 'null'}</span>
      <span data-testid="user">{user ? user.email : 'null'}</span>
      <button type="button" onClick={() => login('jwt', { userId: 1, email: 'u@test.com', displayName: 'U' })}>Login</button>
      <button type="button" onClick={logout}>Logout</button>
    </div>
  )
}

describe('AuthContext', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('provides token and user after login', () => {
    render(
      <AuthProvider>
        <Consumer />
      </AuthProvider>,
    )
    expect(screen.getByTestId('token')).toHaveTextContent('null')
    act(() => {
      screen.getByText('Login').click()
    })
    expect(screen.getByTestId('token')).toHaveTextContent('jwt')
    expect(screen.getByTestId('user')).toHaveTextContent('u@test.com')
  })

  it('clears token and user on logout', () => {
    render(
      <AuthProvider>
        <Consumer />
      </AuthProvider>,
    )
    act(() => screen.getByText('Login').click())
    expect(screen.getByTestId('token')).toHaveTextContent('jwt')
    act(() => screen.getByText('Logout').click())
    expect(screen.getByTestId('token')).toHaveTextContent('null')
    expect(screen.getByTestId('user')).toHaveTextContent('null')
  })
})
