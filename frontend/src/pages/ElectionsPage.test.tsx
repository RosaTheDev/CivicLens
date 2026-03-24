import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import { MantineProvider } from '@mantine/core'
import ElectionsPage from './ElectionsPage'

function renderElectionsPage() {
  return render(
    <MantineProvider>
      <BrowserRouter>
        <ElectionsPage />
      </BrowserRouter>
    </MantineProvider>,
  )
}

describe('ElectionsPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      value: vi.fn().mockImplementation((query: string) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
      })),
    })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('renders election guide and Vote.org resource', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => [],
    } as Response)

    renderElectionsPage()

    expect(await screen.findByText(/Election quick guide/i)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Open Vote.org/i })).toHaveAttribute(
      'href',
      'https://www.vote.org/polling-place-locator/',
    )
  })

  it('loads elections, formats date, and shows live countdown', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => [
        {
          id: 1,
          stateCode: 'NC',
          officeLevel: 'Federal',
          title: 'General Election',
          electionType: 'GENERAL',
          electionDate: '2099-11-03',
          daysUntil: 0,
          description: 'Federal general election.',
        },
      ],
    } as Response)

    renderElectionsPage()

    expect(await screen.findByText('General Election')).toBeInTheDocument()
    expect(screen.getByText('November 3 2099')).toBeInTheDocument()
    expect(screen.getByText(/Live countdown/i)).toBeInTheDocument()
    expect(screen.getByText(/\d+d \d+h \d+m \d+s/)).toBeInTheDocument()
  })

  it('shows empty state after failed fetch and refreshes with uppercase state', async () => {
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce({
        ok: false,
        status: 500,
        json: async () => ({}),
      } as Response)
      .mockResolvedValueOnce({
        ok: true,
        json: async () => [],
      } as Response)

    renderElectionsPage()

    expect(await screen.findByText(/No upcoming elections found/i)).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText(/State/i), { target: { value: 'ca' } })
    fireEvent.click(screen.getByRole('button', { name: /Refresh/i }))

    await waitFor(() => {
      expect(fetchMock).toHaveBeenNthCalledWith(1, '/api/elections?state=NC')
      expect(fetchMock).toHaveBeenNthCalledWith(2, '/api/elections?state=CA')
    })
  })
})
