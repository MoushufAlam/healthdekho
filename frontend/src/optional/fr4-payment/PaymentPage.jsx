// ============================================================================
// OPTIONAL: FR-4 Premium Payment (not evaluated). See paymentApi.js for how to remove it.
// ============================================================================
import { CircleCheckBig, CreditCard, History, ListChecks, ReceiptText, Search, Wallet } from 'lucide-react'
import { useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { toApiError } from '../../api/errors.js'
import Alert from '../../components/ui/Alert.jsx'
import { PolicyTypeBadge, StatusBadge } from '../../components/ui/Badge.jsx'
import Button from '../../components/ui/Button.jsx'
import { Card, CardBody, CardHeader } from '../../components/ui/Card.jsx'
import EmptyState from '../../components/ui/EmptyState.jsx'
import { Field, inputClasses } from '../../components/ui/Field.jsx'
import PageHeader from '../../components/ui/PageHeader.jsx'
import { PageLoader } from '../../components/ui/Spinner.jsx'
import StatCard from '../../components/ui/StatCard.jsx'
import { Table, Tbody, Td, Th, Tr } from '../../components/ui/Table.jsx'
import { useApiData } from '../../hooks/useApiData.js'
import { formatDate, formatEnum, formatFrequency, formatMoney, formatPeriod, formatTerm } from '../../lib/format.js'
import { getInstallments, getPaymentHistory, getPolicyDetail, payInstallments } from './paymentApi.js'

const POLICY_NUMBER_PATTERN = /^[A-Za-z0-9-]{3,30}$/
const PAYMENT_METHODS = { UPI: 'UPI', NET_BANKING: 'Net banking', CARD: 'Card', CASH: 'Cash', CHEQUE: 'Cheque' }
const AMOUNT_PATTERN = /^\d+(\.\d{1,2})?$/

export default function PaymentPage() {
  const { policyNumber } = useParams()
  return (
    <>
      <PageHeader
        eyebrow="Premium payment"
        title="Pay your premium"
        description="Look up a policy to see its payment schedule, then select the installments you want to pay."
      />
      <PolicyLookup current={policyNumber} />
      {policyNumber && <PolicyPayment key={policyNumber} policyNumber={policyNumber} />}
    </>
  )
}

// FR-4A: search by the unique policy identifier
function PolicyLookup({ current }) {
  const navigate = useNavigate()
  const [value, setValue] = useState(current ?? '')
  const [error, setError] = useState(null)

  function handleSubmit(event) {
    event.preventDefault()
    const policyNumber = value.trim().toUpperCase()
    if (!POLICY_NUMBER_PATTERN.test(policyNumber)) {
      setError('Enter a valid policy number, e.g. HDP-2026-0001')
      return
    }
    setError(null)
    navigate(`/payments/${encodeURIComponent(policyNumber)}`)
  }

  return (
    <Card className="mb-8">
      <CardBody>
        <form onSubmit={handleSubmit} noValidate className="flex flex-wrap items-start gap-3">
          <Field label="Policy number" htmlFor="lookupPolicyNumber" error={error} className="min-w-64 flex-1">
            <input
              id="lookupPolicyNumber"
              value={value}
              onChange={(event) => setValue(event.target.value)}
              placeholder="HDP-2026-0001"
              className={`${inputClasses(Boolean(error))} font-mono uppercase`}
            />
          </Field>
          <Button type="submit" icon={Search} className="mt-7">
            Find policy
          </Button>
        </form>
      </CardBody>
    </Card>
  )
}

function PolicyPayment({ policyNumber }) {
  const { data, error, loading, reload } = useApiData(
    () =>
      Promise.all([getPolicyDetail(policyNumber), getInstallments(policyNumber), getPaymentHistory(policyNumber)]).then(
        ([detail, installments, history]) => ({ detail, installments, history }),
      ),
    [policyNumber],
  )

  // installmentId -> amount typed by the user (a string while editing)
  const [selected, setSelected] = useState({})
  const [paymentMethod, setPaymentMethod] = useState('UPI')
  const [paying, setPaying] = useState(false)
  const [payError, setPayError] = useState(null)
  const [lastPayment, setLastPayment] = useState(null)

  if (loading && !data) return <PageLoader label="Loading policy…" />
  if (error) {
    // 404 also covers another customer's policy: the server never reveals that it exists
    return <Alert tone="error" title="Policy not found">{error.message}</Alert>
  }

  const { detail, installments, history } = data
  const selectedIds = Object.keys(selected).map(Number)
  const total = selectedIds.reduce((sum, id) => sum + (Number(selected[id]) || 0), 0)

  function toggle(installment) {
    setPayError(null)
    setSelected((current) => {
      const next = { ...current }
      if (next[installment.installmentId] !== undefined) delete next[installment.installmentId]
      else next[installment.installmentId] = Number(installment.amountDueToday).toFixed(2)
      return next
    })
  }

  async function pay() {
    setPayError(null)
    const invalid = selectedIds.find((id) => !AMOUNT_PATTERN.test(selected[id]) || Number(selected[id]) <= 0)
    if (invalid !== undefined) {
      setPayError('Every selected installment needs an amount greater than 0 with at most 2 decimals.')
      return
    }
    setPaying(true)
    try {
      const response = await payInstallments(policyNumber, {
        paymentMethod,
        installments: selectedIds.map((installmentId) => ({ installmentId, amount: Number(selected[installmentId]) })),
      })
      setLastPayment(response)
      setSelected({})
      reload()
    } catch (err) {
      setPayError(toApiError(err).message) // over-payment, already paid, penalty required...
    } finally {
      setPaying(false)
    }
  }

  return (
    <div className="space-y-8">
      {/* FR-4A: policy header */}
      <Card>
        <CardHeader
          icon={ReceiptText}
          title={detail.policyName}
          subtitle={`${detail.policyNumber} · ${detail.customerName}`}
          action={
            <div className="flex gap-2">
              <PolicyTypeBadge type={detail.policyType} />
              <StatusBadge status={detail.status} />
            </div>
          }
        />
        <CardBody className="grid gap-4 text-sm sm:grid-cols-4">
          <Info label="Term" value={formatTerm(detail.termMonths)} />
          <Info label="Start date" value={formatDate(detail.startDate)} />
          <Info label="Maturity date" value={formatDate(detail.maturityDate)} />
          <Info label="Premium" value={`${formatMoney(detail.premiumAmount)} ${formatFrequency(detail.premiumFrequency).toLowerCase()}`} />
        </CardBody>
      </Card>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard icon={Wallet} label="Total payable" value={formatMoney(detail.totalPremiumPayable)} />
        <StatCard icon={CircleCheckBig} tone="sky" label="Premium paid" value={formatMoney(detail.totalPremiumPaid)} />
        <StatCard icon={ListChecks} tone="amber" label="Remaining" value={formatMoney(detail.totalRemainingPremium)} />
        <StatCard icon={ReceiptText} tone="rose" label="Late penalties paid" value={formatMoney(detail.totalPenaltyPaid)} />
      </div>

      {lastPayment && (
        <Alert tone="success" title={lastPayment.message} onClose={() => setLastPayment(null)}>
          <ul className="mt-1 space-y-0.5">
            {lastPayment.receipts.map((receipt) => (
              <li key={receipt.transactionReference}>
                #{receipt.installmentNumber}: {formatMoney(receipt.amountPaid)}
                {Number(receipt.penaltyAmount) > 0 && ` (incl. penalty ${formatMoney(receipt.penaltyAmount)})`} →{' '}
                {formatEnum(receipt.installmentStatus)} · <span className="font-mono">{receipt.transactionReference}</span>
              </li>
            ))}
          </ul>
        </Alert>
      )}

      {/* FR-4B + 4C: schedule with selection */}
      <Card>
        <CardHeader
          icon={ListChecks}
          title="Premium schedule"
          subtitle="Select pending installments to pay. After the grace period a 10% penalty applies and the full amount is due."
        />
        <Table className="max-h-[32rem] overflow-y-auto">
          <thead>
            <tr>
              <Th className="sticky top-0 z-10">Pay</Th>
              <Th className="sticky top-0 z-10">#</Th>
              <Th className="sticky top-0 z-10">Period</Th>
              <Th className="sticky top-0 z-10" align="right">Premium</Th>
              <Th className="sticky top-0 z-10">Due date</Th>
              <Th className="sticky top-0 z-10">Grace ends</Th>
              <Th className="sticky top-0 z-10" align="right">Paid</Th>
              <Th className="sticky top-0 z-10" align="right">Penalty today</Th>
              <Th className="sticky top-0 z-10" align="right">Due today</Th>
              <Th className="sticky top-0 z-10">Status</Th>
              <Th className="sticky top-0 z-10" align="right">Amount to pay</Th>
            </tr>
          </thead>
          <Tbody>
            {installments.map((installment) => {
              const isSelected = selected[installment.installmentId] !== undefined
              return (
                <Tr key={installment.installmentId} className={isSelected ? 'bg-brand-50/60' : ''}>
                  <Td>
                    <input
                      type="checkbox"
                      checked={isSelected}
                      disabled={!installment.selectable}
                      onChange={() => toggle(installment)}
                      aria-label={`Select installment ${installment.installmentNumber}`}
                      className="size-4 rounded accent-brand-600 disabled:opacity-30"
                    />
                  </Td>
                  <Td className="font-semibold text-slate-900">{installment.installmentNumber}</Td>
                  <Td>{formatPeriod(installment.period)}</Td>
                  <Td align="right">{formatMoney(installment.scheduledAmount)}</Td>
                  <Td>{formatDate(installment.dueDate)}</Td>
                  <Td>{formatDate(installment.gracePeriodEndDate)}</Td>
                  <Td align="right">{formatMoney(installment.paidAmount)}</Td>
                  <Td align="right" className={Number(installment.penaltyIfPaidToday) > 0 ? 'font-semibold text-rose-600' : ''}>
                    {formatMoney(installment.penaltyIfPaidToday)}
                  </Td>
                  <Td align="right" className="font-semibold text-slate-900">{formatMoney(installment.amountDueToday)}</Td>
                  <Td>
                    <StatusBadge status={installment.paymentStatus} />
                  </Td>
                  <Td align="right">
                    {isSelected && (
                      <input
                        type="text"
                        inputMode="decimal"
                        value={selected[installment.installmentId]}
                        onChange={(event) =>
                          setSelected((current) => ({ ...current, [installment.installmentId]: event.target.value }))
                        }
                        aria-label={`Amount for installment ${installment.installmentNumber}`}
                        className={`${inputClasses()} w-32 text-right`}
                      />
                    )}
                  </Td>
                </Tr>
              )
            })}
          </Tbody>
        </Table>

        <div className="flex flex-wrap items-end justify-between gap-4 border-t border-slate-100 px-6 py-5">
          <div className="flex flex-wrap items-end gap-4">
            <Field label="Payment method" htmlFor="paymentMethod">
              <select
                id="paymentMethod"
                value={paymentMethod}
                onChange={(event) => setPaymentMethod(event.target.value)}
                className={`${inputClasses()} w-44`}
              >
                {Object.entries(PAYMENT_METHODS).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
            </Field>
            <div className="pb-1">
              <p className="text-xs text-slate-500">{selectedIds.length} selected · total</p>
              <p className="text-2xl font-bold text-slate-900">{formatMoney(total)}</p>
            </div>
          </div>
          <Button size="lg" icon={CreditCard} loading={paying} disabled={selectedIds.length === 0} onClick={pay}>
            {paying ? 'Processing…' : 'Pay now'}
          </Button>
        </div>
        {payError && (
          <div className="px-6 pb-5">
            <Alert tone="error" title="Payment failed" onClose={() => setPayError(null)}>
              {payError}
            </Alert>
          </div>
        )}
      </Card>

      {/* payment history */}
      <Card>
        <CardHeader icon={History} title="Payment history" subtitle="Every payment recorded for this policy" />
        {history.length === 0 ? (
          <EmptyState icon={History} title="No payments yet" description="Payments you make will show up here." />
        ) : (
          <Table>
            <thead>
              <tr>
                <Th>Transaction</Th>
                <Th>Installment</Th>
                <Th>Date</Th>
                <Th>Method</Th>
                <Th align="right">Penalty</Th>
                <Th align="right">Amount paid</Th>
              </tr>
            </thead>
            <Tbody>
              {history.map((payment) => (
                <Tr key={payment.transactionReference}>
                  <Td className="font-mono text-xs">{payment.transactionReference}</Td>
                  <Td>#{payment.installmentNumber}</Td>
                  <Td>{formatDate(payment.paymentDate)}</Td>
                  <Td>{PAYMENT_METHODS[payment.paymentMethod] ?? formatEnum(payment.paymentMethod)}</Td>
                  <Td align="right">{formatMoney(payment.penaltyAmount)}</Td>
                  <Td align="right" className="font-semibold text-slate-900">{formatMoney(payment.amountPaid)}</Td>
                </Tr>
              ))}
            </Tbody>
          </Table>
        )}
      </Card>
    </div>
  )
}

function Info({ label, value }) {
  return (
    <div>
      <p className="text-slate-500">{label}</p>
      <p className="mt-0.5 font-semibold text-slate-900">{value}</p>
    </div>
  )
}
