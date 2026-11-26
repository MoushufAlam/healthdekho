// ============================================================================
// OPTIONAL: FR-5 Policy Search (not evaluated). See searchApi.js for how to remove it.
// ============================================================================
import { ChevronLeft, ChevronRight, RotateCcw, Search, SearchX } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router'
import Alert from '../../components/ui/Alert.jsx'
import { PolicyTypeBadge, StatusBadge } from '../../components/ui/Badge.jsx'
import Button from '../../components/ui/Button.jsx'
import { Card, CardBody, CardHeader } from '../../components/ui/Card.jsx'
import EmptyState from '../../components/ui/EmptyState.jsx'
import { Field, inputClasses } from '../../components/ui/Field.jsx'
import PageHeader from '../../components/ui/PageHeader.jsx'
import Spinner from '../../components/ui/Spinner.jsx'
import { Table, Tbody, Td, Th, Tr } from '../../components/ui/Table.jsx'
import { useApiData } from '../../hooks/useApiData.js'
import { formatDate, formatMoney, todayIso } from '../../lib/format.js'
import { searchPolicies } from './searchApi.js'

const EMPTY_FORM = { policyNumber: '', customerName: '', dateOfBirth: '' }
const POLICY_NUMBER_PATTERN = /^[A-Za-z0-9-]{3,30}$/
const NAME_PATTERN = /^[\p{L} .'-]{2,100}$/u

// same three allowed combinations as SearchCriteria.java on the server
function validate({ policyNumber, customerName, dateOfBirth }) {
  const number = policyNumber.trim()
  const name = customerName.trim()
  if (!number && !name && !dateOfBirth) return { form: 'Enter a policy number, a customer name, or a policy number with date of birth.' }
  if (number && name) return { form: 'Search by policy number or by customer name, not both.' }
  if (dateOfBirth && !number) return { dateOfBirth: 'Date of birth can only be used together with a policy number' }
  if (number && !POLICY_NUMBER_PATTERN.test(number)) return { policyNumber: "Only letters, digits and '-' (3–30 characters)" }
  if (name && !NAME_PATTERN.test(name)) return { customerName: 'Use 2–100 letters (spaces, . \' and - allowed)' }
  if (dateOfBirth && dateOfBirth > todayIso()) return { dateOfBirth: 'Date of birth cannot be in the future' }
  return {}
}

export default function SearchPage() {
  const [form, setForm] = useState(EMPTY_FORM)
  const [errors, setErrors] = useState({})
  const [query, setQuery] = useState(null) // the submitted search incl. page and size

  const results = useApiData(() => (query ? searchPolicies(query) : null), [query])

  function updateField(event) {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
    setErrors({})
  }

  function handleSubmit(event) {
    event.preventDefault()
    const found = validate(form)
    setErrors(found)
    if (Object.keys(found).length > 0) return
    setQuery({
      policyNumber: form.policyNumber.trim().toUpperCase(),
      customerName: form.customerName.trim(),
      dateOfBirth: form.dateOfBirth,
      page: 0,
      size: query?.size ?? 10,
    })
  }

  function goToPage(page) {
    setQuery((current) => ({ ...current, page }))
  }

  return (
    <>
      <PageHeader
        eyebrow="Policy search"
        title="Search your policies"
        description="Search by policy number, by customer name, or by policy number together with date of birth."
      />

      <Card className="mb-8">
        <CardBody>
          <form onSubmit={handleSubmit} noValidate className="space-y-5">
            {errors.form && <Alert tone="warning">{errors.form}</Alert>}
            <div className="grid gap-5 md:grid-cols-3">
              <Field label="Policy number" htmlFor="policyNumber" error={errors.policyNumber}>
                <input id="policyNumber" name="policyNumber" placeholder="HDP-2026-0001" value={form.policyNumber}
                  onChange={updateField} className={`${inputClasses(Boolean(errors.policyNumber))} font-mono uppercase`} />
              </Field>
              <Field label="Customer name" htmlFor="customerName" error={errors.customerName}>
                <input id="customerName" name="customerName" placeholder="e.g. Aarav" value={form.customerName}
                  onChange={updateField} className={inputClasses(Boolean(errors.customerName))} />
              </Field>
              <Field label="Date of birth" htmlFor="dateOfBirth" error={errors.dateOfBirth} hint="Only together with a policy number">
                <input id="dateOfBirth" name="dateOfBirth" type="date" max={todayIso()} value={form.dateOfBirth}
                  onChange={updateField} className={inputClasses(Boolean(errors.dateOfBirth))} />
              </Field>
            </div>
            <div className="flex flex-wrap gap-3">
              <Button type="submit" icon={Search} loading={results.loading && Boolean(query)}>
                Search
              </Button>
              <Button variant="ghost" icon={RotateCcw} onClick={() => { setForm(EMPTY_FORM); setErrors({}); setQuery(null) }}>
                Clear
              </Button>
            </div>
          </form>
        </CardBody>
      </Card>

      {query && <SearchResults results={results} onPage={goToPage}
        onSize={(size) => setQuery((current) => ({ ...current, size, page: 0 }))} />}
    </>
  )
}

function SearchResults({ results, onPage, onSize }) {
  const { data, error, loading } = results

  if (error) {
    // a policy number that isn't yours comes back as 404, exactly like one that doesn't exist
    return <Alert tone={error.status === 404 ? 'warning' : 'error'} title="No results">{error.message}</Alert>
  }
  if (!data) {
    return <div className="flex justify-center py-12"><Spinner className="size-8 text-brand-600" /></div>
  }

  return (
    <Card className={loading ? 'opacity-60 transition' : 'transition'}>
      <CardHeader icon={Search} title="Search results" subtitle={`${data.totalElements} ${data.totalElements === 1 ? 'policy' : 'policies'} found`} />
      {data.content.length === 0 ? (
        <EmptyState icon={SearchX} title="No policies found" description={data.message} />
      ) : (
        <>
          <Table>
            <thead>
              <tr>
                <Th>Policy ID</Th>
                <Th>Customer</Th>
                <Th>Date of birth</Th>
                <Th>Policy name</Th>
                <Th>Type</Th>
                <Th>Start date</Th>
                <Th>Maturity date</Th>
                <Th align="right">Total payable</Th>
                <Th>Payment status</Th>
                <Th align="right">Paid</Th>
                <Th align="right">Remaining</Th>
              </tr>
            </thead>
            <Tbody>
              {data.content.map((policy) => (
                <Tr key={policy.policyNumber}>
                  <Td className="font-mono text-xs font-semibold">
                    {/* OPTIONAL: FR-4 - link to the payment page */}
                    <Link to={`/payments/${policy.policyNumber}`} className="text-brand-700 hover:underline">
                      {policy.policyNumber}
                    </Link>
                  </Td>
                  <Td className="font-medium text-slate-900">{policy.customerName}</Td>
                  <Td>{formatDate(policy.dateOfBirth)}</Td>
                  <Td>{policy.policyName}</Td>
                  <Td><PolicyTypeBadge type={policy.policyType} /></Td>
                  <Td>{formatDate(policy.startDate)}</Td>
                  <Td>{formatDate(policy.maturityDate)}</Td>
                  <Td align="right">{formatMoney(policy.totalPremiumPayable)}</Td>
                  <Td><StatusBadge status={policy.premiumPaymentStatus} /></Td>
                  <Td align="right">{formatMoney(policy.totalPremiumPaid)}</Td>
                  <Td align="right" className="font-semibold text-slate-900">{formatMoney(policy.remainingPremium)}</Td>
                </Tr>
              ))}
            </Tbody>
          </Table>

          <div className="flex flex-wrap items-center justify-between gap-3 border-t border-slate-100 px-6 py-4 text-sm">
            <label className="flex items-center gap-2 text-slate-500">
              Rows per page
              <select value={data.size} onChange={(event) => onSize(Number(event.target.value))}
                className={`${inputClasses()} w-20 py-1.5`}>
                {[1, 5, 10, 20, 50].map((size) => <option key={size} value={size}>{size}</option>)}
              </select>
            </label>
            <div className="flex items-center gap-3">
              <span className="text-slate-500">Page {data.page + 1} of {Math.max(data.totalPages, 1)}</span>
              <Button variant="secondary" size="sm" icon={ChevronLeft} disabled={data.page === 0 || loading}
                onClick={() => onPage(data.page - 1)}>
                Previous
              </Button>
              <Button variant="secondary" size="sm" disabled={data.page + 1 >= data.totalPages || loading}
                onClick={() => onPage(data.page + 1)}>
                Next <ChevronRight className="size-4" aria-hidden="true" />
              </Button>
            </div>
          </div>
        </>
      )}
    </Card>
  )
}
