import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import { MantineProvider } from '@mantine/core'
import WatchlistPage from './WatchlistPage'
import { AuthProvider } from '../context/AuthContext'

function renderWatchlist() {
  return render(
    <MantineProvider>
      <BrowserRouter>
        <AuthProvider>
          <WatchlistPage />
        </AuthProvider>
      </BrowserRouter>
    </MantineProvider>,
  )
}

describe('WatchlistPage', () => {
  it('shows empty message when watchlist is empty', async () => {
    vi.spyOn(globalThis, 'fetch')
      // first call: watchlist
      .mockResolvedValueOnce({
        ok: true,
        json: async () => [],
      } as any)
      // second call: stances
      .mockResolvedValueOnce({
        ok: true,
        json: async () => [],
      } as any)
    renderWatchlist()
    expect(await screen.findByText(/no representatives on your watch list/i)).toBeInTheDocument()
  })

  it('shows representative when watchlist has items', async () => {
    const items = [
      { id: 1, name: 'Jane Smith', chamber: 'HOUSE', state: 'CA', district: '12', party: 'DEMOCRATIC' },
    ]
    vi.spyOn(globalThis, 'fetch')
      // watchlist
      .mockResolvedValueOnce({
        ok: true,
        json: async () => items,
      } as any)
      // stances
      .mockResolvedValueOnce({
        ok: true,
        json: async () => [],
      } as any)
    renderWatchlist()
    expect(await screen.findByText('Jane Smith')).toBeInTheDocument()
  })
})
