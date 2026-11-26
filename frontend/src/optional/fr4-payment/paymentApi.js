// ============================================================================
// OPTIONAL: FR-4 Premium Payment (not evaluated: only FR-1 to FR-3 are assessed).
// To remove: delete the whole src/optional/fr4-payment folder, then every line tagged
// "OPTIONAL: FR-4" in App.jsx, AppLayout.jsx, DashboardPage.jsx, RegisterPolicyPage.jsx
// and SearchPage.jsx.
// ============================================================================
import { api } from '../../api/client.js'

// policy numbers are put in the URL path, so encode them
const policyUrl = (policyNumber) => `/api/policies/${encodeURIComponent(policyNumber)}`

// FR-4A: policy header with total payable / paid / remaining
export async function getPolicyDetail(policyNumber) {
  const { data } = await api.get(policyUrl(policyNumber))
  return data
}

// FR-4B: installment schedule
export async function getInstallments(policyNumber) {
  const { data } = await api.get(`${policyUrl(policyNumber)}/installments`)
  return data
}

// FR-4C: { paymentMethod, installments: [{ installmentId, amount }] }
export async function payInstallments(policyNumber, request) {
  const { data } = await api.post(`${policyUrl(policyNumber)}/payments`, request)
  return data
}

export async function getPaymentHistory(policyNumber) {
  const { data } = await api.get(`${policyUrl(policyNumber)}/payments`)
  return data
}
