import React from 'react'
import ReactDOM from 'react-dom/client'
import { MantineProvider } from '@mantine/core'
import { BrowserRouter } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import App from './App'
import '@mantine/core/styles.css'
import './cinematic.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <MantineProvider
      theme={{
        primaryColor: 'indigo',
        defaultRadius: 'md',
        fontFamily: '"Public Sans", Inter, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif',
        headings: {
          fontFamily: '"Merriweather", Georgia, Cambria, "Times New Roman", serif',
          fontWeight: '700',
        },
        shadows: {
          md: '0 10px 30px rgba(15, 23, 42, 0.12)',
          xl: '0 20px 50px rgba(15, 23, 42, 0.2)',
        },
      }}
    >
      <BrowserRouter>
        <AuthProvider>
          <App />
        </AuthProvider>
      </BrowserRouter>
    </MantineProvider>
  </React.StrictMode>,
)
