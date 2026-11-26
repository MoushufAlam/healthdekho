import { createContext, startTransition, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router'
import * as authApi from '../api/authApi.js'
import { onUnauthorized } from '../api/client.js'
import { sessionStore } from './sessionStore.js'

const AuthContext = createContext(null)
const EXPIRED_NOTICE = 'Your session has expired. Please log in again.'

export function AuthProvider({ children }) {
  const navigate = useNavigate()
  const [session, setSession] = useState(() => sessionStore.get())

  const logout = useCallback(
    (notice) => {
      sessionStore.clear()
      // React Router navigates inside a transition. Clearing the session in the same transition makes
      // both changes render together; otherwise ProtectedRoute briefly sees "logged out on /register",
      // remembers that page, and the next user who logs in lands there instead of on the dashboard.
      startTransition(() => {
        setSession(null)
        navigate('/login', { replace: true, state: notice ? { notice } : undefined })
      })
    },
    [navigate],
  )

  const login = useCallback(async (email, password) => {
    const { accessToken, expiresAt, customer } = await authApi.login(email, password)
    const newSession = { accessToken, expiresAt, customer }
    sessionStore.set(newSession)
    setSession(newSession)
    return newSession
  }, [])

  // any API call that comes back 401 ends the session
  useEffect(() => {
    onUnauthorized(() => logout(EXPIRED_NOTICE))
  }, [logout])

  // log out exactly when the JWT expires instead of waiting for the next failing request
  useEffect(() => {
    if (!session) return undefined
    const msLeft = new Date(session.expiresAt).getTime() - Date.now()
    const timer = setTimeout(() => logout(EXPIRED_NOTICE), Math.max(msLeft, 0))
    return () => clearTimeout(timer)
  }, [session, logout])

  const value = useMemo(
    () => ({ customer: session?.customer ?? null, isAuthenticated: session !== null, login, logout }),
    [session, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used inside <AuthProvider>')
  }
  return context
}
