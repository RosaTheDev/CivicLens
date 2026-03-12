import React, { ReactElement } from 'react'
import { render, RenderOptions } from '@testing-library/react'
import { MantineProvider } from '@mantine/core'
import { BrowserRouter } from 'react-router-dom'
import { AuthProvider } from '../context/AuthContext'

function AllTheProviders({ children }: { children: React.ReactNode }) {
  return (
    <MantineProvider>
      <BrowserRouter>
        <AuthProvider>
          {children}
        </AuthProvider>
      </BrowserRouter>
    </MantineProvider>
  )
}

function renderWithProviders(ui: ReactElement, options?: Omit<RenderOptions, 'wrapper'>) {
  return render(ui, {
    wrapper: AllTheProviders,
    ...options,
  })
}

export * from '@testing-library/react'
export { renderWithProviders as render }
