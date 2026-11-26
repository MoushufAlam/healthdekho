import {
  ArrowLeft,
  CalendarDays,
  CircleCheckBig,
  ClipboardCheck,
  Copy,
  FileText,
  LayoutDashboard,
  PackageSearch,
  ShieldCheck,
  Sparkles,
  UserRound,
  Wallet,
} from 'lucide-react'
import { useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { getAvailableProducts } from '../api/customerApi.js'
import { toApiError } from '../api/errors.js'
import { previewRegistration, registerPolicy } from '../api/policyApi.js'
import Alert from '../components/ui/Alert.jsx'
import { Badge, PolicyTypeBadge } from '../components/ui/Badge.jsx'
import Button, { ButtonLink } from '../components/ui/Button.jsx'
import { Card, CardBody, CardHeader } from '../components/ui/Card.jsx'
import EmptyState from '../components/ui/EmptyState.jsx'
import { Field, inputClasses, ReadOnlyField } from '../components/ui/Field.jsx'
import PageHeader from '../components/ui/PageHeader.jsx'
import Spinner, { PageLoader } from '../components/ui/Spinner.jsx'
import { useApiData } from '../hooks/useApiData.js'
import { formatDate, formatFrequency, formatMoney, formatMoneyWhole, formatTerm, todayIso } from '../lib/format.js'

function validateStartDate(startDate) {
  if (!startDate) return 'Policy start date is required'
  if (startDate < todayIso()) return 'Policy start date cannot be in the past'
  return null
}

// FR-3: policy registration. Reached from "Register" on the dashboard (?productId=)
// or from an AI recommendation (?productId=&recommendationItemId=).
export default function RegisterPolicyPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const productId = searchParams.get('productId')
  const recommendationItemId = searchParams.get('recommendationItemId')

  if (!productId) {
    return <ProductPicker onSelect={(id) => setSearchParams({ productId: String(id) })} />
  }
  // key: picking another product starts a fresh form instead of reusing the old state
  return <RegistrationForm key={productId} productId={productId} recommendationItemId={recommendationItemId} />
}

function RegistrationForm({ productId, recommendationItemId }) {
  const [startDate, setStartDate] = useState(todayIso()) // FR-3A: defaults to today
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState(null)
  const [registered, setRegistered] = useState(null)

  const startDateError = validateStartDate(startDate)

  // The preview is re-calculated by the server on every start date change, so the maturity date,
  // total premium and eligibility shown here always come from the same Java code that registers.
  const preview = useApiData(
    () => (startDate ? previewRegistration(productId, startDate) : null),
    [productId, startDate],
  )

  // keep showing the last good preview while the date field is temporarily empty
  const lastPreview = useRef(null)
  if (preview.data) lastPreview.current = preview.data
  const data = preview.data ?? lastPreview.current

  async function handleSubmit(event) {
    event.preventDefault()
    setSubmitError(null)
    if (startDateError || !data?.eligible) return

    setSubmitting(true)
    try {
      const response = await registerPolicy({
        productId: Number(productId),
        startDate,
        recommendationItemId: recommendationItemId ? Number(recommendationItemId) : undefined,
      })
      setRegistered(response)
      window.scrollTo({ top: 0, behavior: 'smooth' })
    } catch (err) {
      setSubmitError(toApiError(err).message) // e.g. 409 duplicate policy, 400 age outside the entry range
      preview.reload() // the eligibility shown may be out of date now
    } finally {
      setSubmitting(false)
    }
  }

  if (registered) {
    return <RegistrationSuccess registration={registered} />
  }

  if (!data && preview.loading) {
    return <PageLoader label="Preparing your registration form…" />
  }

  if (!data && preview.error) {
    return (
      <>
        <BackLink />
        <Alert tone="error" title="This product can't be registered">
          {preview.error.message}
        </Alert>
      </>
    )
  }

  if (!data) return null

  const { customer, product } = data
  const canSubmit = data.eligible && !startDateError && !preview.loading

  return (
    <>
      <BackLink />
      <PageHeader
        eyebrow="Policy registration"
        title={`Register ${product.policyName}`}
        description="Your personal details and the product details are filled in for you. Choose a start date and we calculate the rest."
        actions={
          recommendationItemId && (
            <Badge tone="violet" icon={Sparkles}>
              From your AI recommendation
            </Badge>
          )
        }
      />

      <form onSubmit={handleSubmit} noValidate className="grid items-start gap-6 lg:grid-cols-3">
        <div className="space-y-6 lg:col-span-2">
          <Card>
            <CardHeader icon={UserRound} title="Your details" subtitle="Auto-filled from your customer profile" />
            <CardBody className="grid gap-5 sm:grid-cols-2">
              <ReadOnlyField label="Customer name" value={customer.fullName} emphasis />
              <ReadOnlyField label="Date of birth" value={`${formatDate(customer.dateOfBirth)} (age ${customer.age})`} />
              <ReadOnlyField label="Email" value={customer.email} />
              <ReadOnlyField label="Phone" value={customer.phone} />
            </CardBody>
          </Card>

          <Card>
            <CardHeader icon={ShieldCheck} title="Policy details" subtitle="Taken from the selected insurance product" />
            <CardBody className="grid gap-5 sm:grid-cols-2">
              <ReadOnlyField label="Policy name" value={product.policyName} emphasis />
              <div>
                <p className="mb-1.5 text-sm font-medium text-slate-500">Policy type</p>
                <div className="py-2">
                  <PolicyTypeBadge type={product.policyType} />
                </div>
              </div>
              <ReadOnlyField label="Product code" value={product.productCode} />
              <ReadOnlyField label="Policy term" value={formatTerm(product.termMonths)} />
              <ReadOnlyField label="Premium frequency" value={formatFrequency(product.premiumFrequency)} />
              <ReadOnlyField label="Premium amount" value={formatMoney(product.premiumAmount)} />
              <ReadOnlyField label="Sum insured" value={formatMoneyWhole(product.sumInsured)} />
              <ReadOnlyField label="Entry age" value={`${product.minEntryAge} – ${product.maxEntryAge} years`} />
            </CardBody>
          </Card>

          <Card>
            <CardHeader icon={CalendarDays} title="Policy dates" subtitle="Pick when your cover should start" />
            <CardBody className="grid gap-5 sm:grid-cols-2">
              <Field
                label="Policy start date"
                htmlFor="startDate"
                required
                error={startDateError}
                hint="Defaults to today; can't be in the past"
              >
                <input
                  id="startDate"
                  type="date"
                  min={todayIso()}
                  value={startDate}
                  onChange={(event) => setStartDate(event.target.value)}
                  aria-invalid={Boolean(startDateError)}
                  aria-describedby="startDate-message"
                  className={inputClasses(Boolean(startDateError))}
                />
              </Field>
              <ReadOnlyField label="Policy maturity date (calculated)" value={formatDate(data.maturityDate)} emphasis />
            </CardBody>
          </Card>
        </div>

        {/* summary column, sticks to the top while scrolling on large screens */}
        <div className="space-y-4 lg:sticky lg:top-24">
          <Card className="overflow-hidden">
            <div className="bg-linear-to-br from-brand-600 to-brand-800 px-6 py-6 text-white">
              <p className="text-sm font-medium text-brand-100">Total premium payable</p>
              <p className="mt-1 text-3xl font-extrabold tracking-tight">{formatMoney(data.totalPremiumPayable)}</p>
              <p className="mt-1 text-sm text-brand-100">
                {data.numberOfInstallments} × {formatMoney(product.premiumAmount)} ({formatFrequency(product.premiumFrequency).toLowerCase()})
              </p>
            </div>
            <dl className="divide-y divide-slate-100 px-6 text-sm">
              <SummaryRow label="Start date" value={formatDate(data.startDate)} />
              <SummaryRow label="Maturity date" value={formatDate(data.maturityDate)} />
              <SummaryRow label="Installments" value={data.numberOfInstallments} />
              <SummaryRow label="First due date" value={formatDate(data.firstDueDate)} />
              <SummaryRow label="Last due date" value={formatDate(data.lastDueDate)} />
              <SummaryRow label="Sum insured" value={formatMoneyWhole(product.sumInsured)} />
            </dl>
            <div className="space-y-3 border-t border-slate-100 px-6 py-5">
              {preview.loading ? (
                <p className="flex items-center gap-2 text-sm text-slate-500">
                  <Spinner className="size-4" /> Recalculating…
                </p>
              ) : (
                <Alert tone={data.eligible ? 'success' : 'warning'}>{data.eligibilityMessage}</Alert>
              )}
              {submitError && (
                <Alert tone="error" title="Registration failed" onClose={() => setSubmitError(null)}>
                  {submitError}
                </Alert>
              )}
              <Button type="submit" size="lg" icon={ClipboardCheck} loading={submitting} disabled={!canSubmit} className="w-full">
                {submitting ? 'Registering…' : 'Confirm registration'}
              </Button>
              <p className="text-center text-xs text-slate-500">
                Your premium schedule is created automatically after registration.
              </p>
            </div>
          </Card>
        </div>
      </form>
    </>
  )
}

function SummaryRow({ label, value }) {
  return (
    <div className="flex items-center justify-between gap-4 py-3">
      <dt className="text-slate-500">{label}</dt>
      <dd className="font-semibold text-slate-900">{value}</dd>
    </div>
  )
}

function BackLink() {
  return (
    <Link to="/dashboard" className="mb-4 inline-flex items-center gap-1.5 text-sm font-medium text-slate-500 hover:text-slate-900">
      <ArrowLeft className="size-4" aria-hidden="true" /> Back to dashboard
    </Link>
  )
}

function RegistrationSuccess({ registration }) {
  const [copied, setCopied] = useState(false)

  async function copyPolicyNumber() {
    await navigator.clipboard?.writeText(registration.policyNumber)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <div className="mx-auto max-w-2xl animate-fade-in">
      <Card className="overflow-hidden text-center">
        <div className="bg-linear-to-b from-emerald-50 to-white px-6 pb-6 pt-10">
          <span className="mx-auto grid size-16 place-items-center rounded-full bg-emerald-100 text-emerald-600 ring-8 ring-emerald-50">
            <CircleCheckBig className="size-8" aria-hidden="true" />
          </span>
          <h1 className="mt-6 text-2xl font-bold text-slate-900">Policy registered!</h1>
          <p className="mt-2 text-slate-600">{registration.message}</p>

          <div className="mx-auto mt-6 inline-flex items-center gap-3 rounded-2xl bg-white px-5 py-3 ring-1 ring-slate-200">
            <div className="text-left">
              <p className="text-xs font-medium uppercase tracking-wide text-slate-500">Policy number</p>
              <p className="font-mono text-xl font-bold text-slate-900">{registration.policyNumber}</p>
            </div>
            <button
              type="button"
              onClick={copyPolicyNumber}
              className="rounded-lg p-2 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700"
              aria-label="Copy policy number"
            >
              {copied ? <ClipboardCheck className="size-5 text-emerald-600" /> : <Copy className="size-5" />}
            </button>
          </div>
        </div>

        <dl className="grid grid-cols-2 gap-px border-y border-slate-100 bg-slate-100 sm:grid-cols-4">
          {[
            ['Policy', registration.policy.policyName],
            ['Installments', registration.numberOfInstallments],
            ['Total payable', formatMoney(registration.totalPremiumPayable)],
            ['First due', formatDate(registration.firstDueDate)],
          ].map(([label, value]) => (
            <div key={label} className="bg-white px-4 py-4">
              <dt className="text-xs text-slate-500">{label}</dt>
              <dd className="mt-1 text-sm font-semibold text-slate-900">{value}</dd>
            </div>
          ))}
        </dl>

        <div className="flex flex-wrap justify-center gap-3 px-6 py-6">
          <ButtonLink to="/dashboard" icon={LayoutDashboard}>
            Go to dashboard
          </ButtonLink>
          {/* OPTIONAL: FR-4 */}
          <ButtonLink to={`/payments/${registration.policyNumber}`} variant="secondary" icon={Wallet}>
            View payment schedule
          </ButtonLink>
        </div>
      </Card>
    </div>
  )
}

// Shown when /register is opened without a product (e.g. from the "New Policy" menu item)
function ProductPicker({ onSelect }) {
  const { data: products, error, loading } = useApiData(() => getAvailableProducts(), [])

  return (
    <>
      <PageHeader
        eyebrow="Policy registration"
        title="Choose a product"
        description="Pick the insurance product you want to register. Products you already hold are not listed."
        actions={
          <ButtonLink to="/recommendations" variant="ai" icon={Sparkles}>
            Not sure? Ask the AI
          </ButtonLink>
        }
      />
      {loading && <PageLoader label="Loading products…" />}
      {error && <Alert tone="error">{error.message}</Alert>}
      {products && products.length === 0 && (
        <Card>
          <EmptyState icon={PackageSearch} title="Nothing left to register" description="You already hold every product we offer." />
        </Card>
      )}
      {products && products.length > 0 && (
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {products.map((product) => (
            <button
              key={product.productId}
              type="button"
              onClick={() => onSelect(product.productId)}
              className="group flex flex-col rounded-2xl bg-white p-6 text-left shadow-card ring-1 ring-slate-200/70 transition hover:-translate-y-0.5 hover:ring-brand-300"
            >
              <div className="flex items-center justify-between">
                <PolicyTypeBadge type={product.policyType} />
                <span className="font-mono text-xs text-slate-400">{product.productCode}</span>
              </div>
              <h3 className="mt-4 text-lg font-semibold text-slate-900">{product.policyName}</h3>
              <p className="mt-1 text-sm text-slate-500">
                {formatTerm(product.termMonths)} · {formatFrequency(product.premiumFrequency)} premium
              </p>
              <div className="mt-5 flex items-end justify-between">
                <div>
                  <p className="text-xs text-slate-500">Premium</p>
                  <p className="text-lg font-bold text-slate-900">{formatMoney(product.premiumAmount)}</p>
                </div>
                <div className="text-right">
                  <p className="text-xs text-slate-500">Cover</p>
                  <p className="text-sm font-semibold text-slate-700">{formatMoneyWhole(product.sumInsured)}</p>
                </div>
              </div>
              <span className="mt-5 inline-flex items-center gap-1.5 text-sm font-semibold text-brand-700 group-hover:text-brand-800">
                <FileText className="size-4" aria-hidden="true" /> Register this product
              </span>
            </button>
          ))}
        </div>
      )}
    </>
  )
}
