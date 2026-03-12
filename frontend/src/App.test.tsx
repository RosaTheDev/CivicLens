import { describe, it, expect } from 'vitest'
import { render, screen } from './test/test-utils'
import App from './App'

function renderApp() {
  return render(<App />)
}

describe('App', () => {
  it('shows login or app content', () => {
    renderApp()
    const signIn = screen.queryByRole('button', { name: /sign in/i })
    const title = screen.queryByText('CivicLens')
    expect(signIn != null || title != null).toBe(true)
  })
})
