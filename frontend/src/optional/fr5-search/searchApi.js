// ============================================================================
// OPTIONAL: FR-5 Policy Search (not evaluated: only FR-1 to FR-3 are assessed).
// To remove: delete the whole src/optional/fr5-search folder, then the lines tagged
// "OPTIONAL: FR-5" in App.jsx and AppLayout.jsx.
// ============================================================================
import { api } from '../../api/client.js'

// { policyNumber?, customerName?, dateOfBirth?, page, size } -> { content[], page, size, totalElements, totalPages, message? }
export async function searchPolicies({ policyNumber, customerName, dateOfBirth, page, size }) {
  const params = { page, size }
  if (policyNumber) params.policyNumber = policyNumber
  if (customerName) params.customerName = customerName
  if (dateOfBirth) params.dateOfBirth = dateOfBirth
  const { data } = await api.get('/api/policies/search', { params })
  return data
}
