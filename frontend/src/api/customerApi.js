import { api } from './client.js'

export async function getProfile() {
  const { data } = await api.get('/api/customers/me')
  return data
}

// FR-1B. { customerName, totalPolicies, policies[], message? }
export async function getDashboard() {
  const { data } = await api.get('/api/dashboard')
  return data
}

// FR-1C. Active products the customer doesn't hold yet
export async function getAvailableProducts() {
  const { data } = await api.get('/api/products/available')
  return data
}
