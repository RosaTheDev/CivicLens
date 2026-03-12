import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import { MantineProvider } from '@mantine/core'
import DashboardPage from './DashboardPage'
import { AuthProvider } from '../context/AuthContext'

function renderDashboard() {
  return render(
    <MantineProvider>
      <BrowserRouter>
        <AuthProvider>
          <DashboardPage />
        </AuthProvider>
      </BrowserRouter>
    </MantineProvider>,
  )
}

describe('DashboardPage', () => {
  it('renders ZIP input and search button', () => {
    renderDashboard()
    expect(screen.getByPlaceholderText(/zip/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /search/i })).toBeInTheDocument()
  })

  it('shows representative after search', async () => {
    const reps = [
      { id: 1, name: 'Jane Smith', chamber: 'HOUSE', state: 'CA', district: '12', party: 'DEMOCRATIC' },
    ]
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => reps,
    } as any)

    renderDashboard()
    fireEvent.change(screen.getByPlaceholderText(/zip/i), { target: { value: '94110' } })
    fireEvent.click(screen.getByRole('button', { name: /search/i }))
    expect(await screen.findByText('Jane Smith')).toBeInTheDocument()
  })
})
