import { api } from './client.js'

// FR-2. Every field is optional but at least one must be sent; blank fields are dropped first
// so the server never sees "" (which would fail enum/date parsing).
export async function getRecommendations({ dateOfBirth, insuranceGoals, preferredTerm, preferredPolicyType }) {
  const body = Object.fromEntries(
    Object.entries({ dateOfBirth, insuranceGoals, preferredTerm, preferredPolicyType })
      .map(([key, value]) => [key, typeof value === 'string' ? value.trim() : value])
      .filter(([, value]) => value !== '' && value != null),
  )
  const { data } = await api.post('/api/recommendations', body)
  return data
}
