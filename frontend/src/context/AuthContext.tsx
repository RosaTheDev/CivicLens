import { createContext, useContext, useState, useCallback, ReactNode } from 'react'

const AUTH_KEY = 'civiclens_token'
const USER_KEY = 'civiclens_user'

type UserInfo = { userId: number; email: string; displayName?: string } | null

type AuthContextType = {
  token: string | null
  user: UserInfo
  login: (token: string, user: UserInfo) => void
  logout: () => void
  setToken: (token: string | null) => void
}

const AuthContext = createContext<AuthContextType | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setTokenState] = useState<string | null>(() => localStorage.getItem(AUTH_KEY))
  const [user, setUser] = useState<UserInfo>(() => {
    try {
      const raw = localStorage.getItem(USER_KEY)
      return raw ? JSON.parse(raw) : null
    } catch {
      return null
    }
  })

  const setToken = useCallback((t: string | null) => {
    setTokenState(t)
    if (t) localStorage.setItem(AUTH_KEY, t)
    else localStorage.removeItem(AUTH_KEY)
  }, [])

  const login = useCallback((t: string, u: UserInfo) => {
    setTokenState(t)
    setUser(u)
    localStorage.setItem(AUTH_KEY, t)
    if (u) localStorage.setItem(USER_KEY, JSON.stringify(u))
  }, [])

  const logout = useCallback(() => {
    setTokenState(null)
    setUser(null)
    localStorage.removeItem(AUTH_KEY)
    localStorage.removeItem(USER_KEY)
  }, [])

  return (
    <AuthContext.Provider value={{ token, user, login, logout, setToken }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
