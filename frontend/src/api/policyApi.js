import { api } from './client.js'

// FR-3A. Pre-filled form data: customer, product, maturity date, total premium and eligibility.
// Nothing is saved; it's called again whenever the start date changes.
export async function previewRegistration(productId, startDate) {
  const { data } = await api.get('/api/policies/preview', { params: { productId, startDate } })
  return data
}

// FR-3B. { productId, startDate, recommendationItemId? } -> 201 with the new policy number
export async function registerPolicy(request) {
  const { data } = await api.post('/api/policies', request)
  return data
}
