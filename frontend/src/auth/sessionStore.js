const KEY = 'healthdekho.session'

// sessionStorage (not localStorage): the login survives a page refresh but ends when the tab is closed.
// Stored shape: { accessToken, expiresAt, customer }
export const sessionStore = {
  get() {
    try {
      const session = JSON.parse(sessionStorage.getItem(KEY))
      if (!session) return null
      if (new Date(session.expiresAt) <= new Date()) {
        sessionStorage.removeItem(KEY)
        return null
      }
      return session
    } catch {
      return null // corrupted value: treat as logged out
    }
  },
  set(session) {
    sessionStorage.setItem(KEY, JSON.stringify(session))
  },
  clear() {
    sessionStorage.removeItem(KEY)
  },
}
