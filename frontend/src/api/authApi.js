import { api } from './client.js'

// FR-1A. Returns { accessToken, tokenType, expiresAt, customer }
export async function login(email, password) {
  const { data } = await api.post('/api/auth/login', { email, password })
  return data
}
