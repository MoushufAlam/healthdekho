import axios from 'axios'
import { sessionStore } from '../auth/sessionStore.js'

// One configured Axios instance for the whole app; every api/*.js module uses it.
export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080',
  // the server gives the AI up to 20 s before falling back, so allow a bit more here
  timeout: 30_000,
})

// Attach the JWT to every request. The backend reads the customer id from it,
// which is why no API call ever sends a customer id.
api.interceptors.request.use((config) => {
  const token = sessionStore.get()?.accessToken
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let handleUnauthorized = () => {}

// AuthContext registers a handler that logs the user out
export function onUnauthorized(handler) {
  handleUnauthorized = handler
}

api.interceptors.response.use(
  (response) => response,
  (error) => {
    // a 401 from the login call is just "wrong password"; anywhere else the token is missing or expired
    const isLoginCall = error.config?.url?.includes('/api/auth/login')
    if (error.response?.status === 401 && !isLoginCall) {
      handleUnauthorized()
    }
    return Promise.reject(error)
  },
)
