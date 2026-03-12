import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { MantineProvider } from '@mantine/core'
import RepresentativeDetailPage from './RepresentativeDetailPage'
import { AuthProvider } from '../context/AuthContext'

function renderDetail() {
  return render(
    <MantineProvider>
      <AuthProvider>
        <MemoryRouter initialEntries={['/representatives/1']}>
          <Routes>
            <Route path="/representatives/:id" element={<RepresentativeDetailPage />} />
          </Routes>
        </MemoryRouter>
      </AuthProvider>
    </MantineProvider>,
  )
}

describe('RepresentativeDetailPage', () => {
  it('renders representative info, donor summary, stance text and recent bills', async () => {
    const rep = {
      id: 1,
      name: 'Jane Smith',
      chamber: 'HOUSE',
      state: 'CA',
      district: '12',
      party: 'DEMOCRATIC',
      officialUrl: 'https://example.com/jane',
    }
    const donor = {
      id: 1,
      representativeId: 1,
      cycleYear: 2024,
      source: 'FEC (mock)',
      totalAmount: 500000,
      topIndustry1: 'Finance',
      topIndustry2: 'Healthcare',
    }
    const bills = [
      {
        url: 'https://www.congress.gov/bill/1',
        title: 'Bill One',
        description: 'First bill description',
      },
    ]
    const stance = {
      representativeId: 1,
      stance: 'SUPPORT',
      note: null,
    }

    vi.spyOn(globalThis, 'fetch')
      // representative
      .mockResolvedValueOnce({
        ok: true,
        json: async () => rep,
      } as any)
      // donor summary
      .mockResolvedValueOnce({
        ok: true,
        json: async () => donor,
      } as any)
      // watchlist
      .mockResolvedValueOnce({
        ok: true,
        json: async () => [],
      } as any)
      // recent bills
      .mockResolvedValueOnce({
        ok: true,
        json: async () => bills,
      } as any)
      // stance
      .mockResolvedValueOnce({
        ok: true,
        json: async () => stance,
      } as any)

    renderDetail()

    expect(await screen.findByText('Jane Smith')).toBeInTheDocument()
    expect(screen.getByText(/Donor summary/i)).toBeInTheDocument()
    expect(screen.getByText(/Bill One/)).toBeInTheDocument()
    expect(screen.getByText('I SUPPORT')).toBeInTheDocument()
  })

  it('saves stance and note and shows note instead of stance text', async () => {
    const rep = {
      id: 1,
      name: 'Jane Smith',
      chamber: 'HOUSE',
      state: 'CA',
      district: '12',
      party: 'DEMOCRATIC',
      officialUrl: 'https://example.com/jane',
    }

    vi.spyOn(globalThis, 'fetch')
      // initial representative load
      .mockResolvedValueOnce({
        ok: true,
        json: async () => rep,
      } as any)
      // donor summary (none)
      .mockResolvedValueOnce({
        ok: false,
        json: async () => null,
      } as any)
      // watchlist (empty)
      .mockResolvedValueOnce({
        ok: true,
        json: async () => [],
      } as any)
      // recent bills (empty)
      .mockResolvedValueOnce({
        ok: true,
        json: async () => [],
      } as any)
      // existing stance (none)
      .mockResolvedValueOnce({
        ok: false,
        json: async () => null,
      } as any)
      // save stance
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({}),
      } as any)

    renderDetail()

    expect(await screen.findByText('Jane Smith')).toBeInTheDocument()

    fireEvent.change(screen.getByPlaceholderText(/Support \/ Oppose \/ Neutral/i), {
      target: { value: 'OPPOSE' },
    })
    fireEvent.change(screen.getByPlaceholderText(/Note \(optional\)/i), {
      target: { value: 'Needs more transparency' },
    })
    fireEvent.click(screen.getByRole('button', { name: /save stance/i }))

    expect(await screen.findByText('Needs more transparency')).toBeInTheDocument()
  })
})

