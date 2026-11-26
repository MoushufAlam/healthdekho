import { ArrowRight, FileText, Layers, PackageSearch, RefreshCw, ShieldCheck, Sparkles, Umbrella, Wallet } from 'lucide-react'
import { Link } from 'react-router'
import { getAvailableProducts, getDashboard } from '../api/customerApi.js'
import Alert from '../components/ui/Alert.jsx'
import { PolicyTypeBadge, StatusBadge } from '../components/ui/Badge.jsx'
import Button, { ButtonLink } from '../components/ui/Button.jsx'
import { Card, CardHeader } from '../components/ui/Card.jsx'
import EmptyState from '../components/ui/EmptyState.jsx'
import PageHeader from '../components/ui/PageHeader.jsx'
import { PageLoader } from '../components/ui/Spinner.jsx'
import StatCard from '../components/ui/StatCard.jsx'
import { Table, Tbody, Td, Th, Tr } from '../components/ui/Table.jsx'
import { useApiData } from '../hooks/useApiData.js'
import { firstName, formatDate, formatFrequency, formatMoney, formatMoneyWhole, formatTerm } from '../lib/format.js'

// FR-1B (my policies) + FR-1C (available products) + the FR-2 entry point
export default function DashboardPage() {
  // both calls run in parallel; the page waits for both
  const { data, error, loading, reload } = useApiData(
    () => Promise.all([getDashboard(), getAvailableProducts()]).then(([dashboard, products]) => ({ dashboard, products })),
    [],
  )

  if (loading && !data) {
    return <PageLoader label="Loading your dashboard…" />
  }

  if (error) {
    return (
      <Alert tone="error" title="Could not load your dashboard">
        <p>{error.message}</p>
        <Button variant="secondary" size="sm" icon={RefreshCw} onClick={reload} className="mt-3">
          Try again
        </Button>
      </Alert>
    )
  }

  const { dashboard, products } = data
  const activePolicies = dashboard.policies.filter((policy) => policy.status === 'ACTIVE').length
  const totalCover = dashboard.policies.reduce((sum, policy) => sum + Number(policy.sumInsured), 0)

  return (
    <>
      <PageHeader
        eyebrow={new Date().toLocaleDateString('en-IN', { weekday: 'long', day: 'numeric', month: 'long' })}
        title={`Welcome back, ${firstName(dashboard.customerName)} 👋`}
        description="Here is an overview of your insurance policies and products you can still add."
        actions={
          <ButtonLink to="/recommendations" variant="ai" icon={Sparkles}>
            Get AI recommendations
          </ButtonLink>
        }
      />

      <div className="mb-8 grid gap-4 sm:grid-cols-3">
        <StatCard icon={FileText} label="My policies" value={dashboard.totalPolicies} hint={`${activePolicies} active`} />
        <StatCard icon={Umbrella} tone="sky" label="Total cover" value={formatMoneyWhole(totalCover)} hint="Sum insured across all policies" />
        <StatCard icon={Layers} tone="amber" label="Products available" value={products.length} hint="Not yet in your portfolio" />
      </div>

      {/* FR-2: the recommendation option lives on the dashboard */}
      <div className="relative mb-8 overflow-hidden rounded-2xl bg-linear-to-r from-violet-600 via-indigo-600 to-brand-600 p-6 text-white shadow-card sm:p-8">
        <div className="absolute -right-10 -top-16 size-64 rounded-full bg-white/10 blur-2xl" aria-hidden="true" />
        <div className="relative flex flex-wrap items-center justify-between gap-6">
          <div className="max-w-xl">
            <p className="inline-flex items-center gap-2 rounded-full bg-white/15 px-3 py-1 text-xs font-semibold">
              <Sparkles className="size-3.5" aria-hidden="true" /> AI Insurance Advisor
            </p>
            <h2 className="mt-3 text-xl font-bold sm:text-2xl">Not sure which cover you need?</h2>
            <p className="mt-1 text-sm text-white/85 sm:text-base">
              Tell us your goals and we&apos;ll rank the products that suit you best, with a reason for each.
            </p>
          </div>
          <ButtonLink to="/recommendations" variant="white" size="lg" icon={ArrowRight}>
            Get recommendations
          </ButtonLink>
        </div>
      </div>

      {/* FR-1B: my policies */}
      <Card className="mb-8">
        <CardHeader
          icon={ShieldCheck}
          title="My policies"
          subtitle="All insurance policies registered in your name"
        />
        {dashboard.policies.length === 0 ? (
          <EmptyState
            icon={FileText}
            title="No policies yet"
            description={dashboard.message}
            action={
              <Button
                variant="secondary"
                icon={ArrowRight}
                onClick={() => document.getElementById('available-products')?.scrollIntoView({ behavior: 'smooth' })}
              >
                Explore products
              </Button>
            }
          />
        ) : (
          <Table>
            <thead>
              <tr>
                <Th>Policy ID</Th>
                <Th>Policy name</Th>
                <Th>Type</Th>
                <Th>Term</Th>
                <Th>Start date</Th>
                <Th>Maturity date</Th>
                <Th>Frequency</Th>
                <Th align="right">Premium</Th>
                <Th align="right">Sum insured</Th>
                <Th>Status</Th>
                {/* OPTIONAL: FR-4 */}
                <Th align="right">Action</Th>
              </tr>
            </thead>
            <Tbody>
              {dashboard.policies.map((policy) => (
                <Tr key={policy.policyNumber}>
                  <Td className="font-mono text-xs font-semibold text-slate-900">{policy.policyNumber}</Td>
                  <Td className="font-medium text-slate-900">{policy.policyName}</Td>
                  <Td>
                    <PolicyTypeBadge type={policy.policyType} />
                  </Td>
                  <Td>{formatTerm(policy.termMonths)}</Td>
                  <Td>{formatDate(policy.startDate)}</Td>
                  <Td>{formatDate(policy.maturityDate)}</Td>
                  <Td>{formatFrequency(policy.premiumFrequency)}</Td>
                  <Td align="right">{formatMoney(policy.premiumAmount)}</Td>
                  <Td align="right">{formatMoneyWhole(policy.sumInsured)}</Td>
                  <Td>
                    <StatusBadge status={policy.status} />
                  </Td>
                  {/* OPTIONAL: FR-4 - shortcut to the payment page */}
                  <Td align="right">
                    <Link
                      to={`/payments/${policy.policyNumber}`}
                      className="inline-flex items-center gap-1 text-sm font-semibold text-brand-700 hover:text-brand-800"
                    >
                      <Wallet className="size-4" aria-hidden="true" /> Pay
                    </Link>
                  </Td>
                </Tr>
              ))}
            </Tbody>
          </Table>
        )}
      </Card>

      {/* FR-1C: products the customer doesn't hold yet */}
      <Card id="available-products" className="scroll-mt-24">
        <CardHeader
          icon={PackageSearch}
          title="Available insurance products"
          subtitle="Products you already hold are hidden (one policy per product)"
        />
        {products.length === 0 ? (
          <EmptyState
            icon={PackageSearch}
            title="You hold every product we offer"
            description="Great coverage! New products will appear here when they launch."
          />
        ) : (
          <Table>
            <thead>
              <tr>
                <Th>Product code</Th>
                <Th>Policy name</Th>
                <Th>Type</Th>
                <Th>Term</Th>
                <Th align="right">Sum insured</Th>
                <Th>Frequency</Th>
                <Th align="right">Premium</Th>
                <Th align="right">Action</Th>
              </tr>
            </thead>
            <Tbody>
              {products.map((product) => (
                <Tr key={product.productId}>
                  <Td className="font-mono text-xs font-semibold text-slate-900">{product.productCode}</Td>
                  <Td className="font-medium text-slate-900">{product.policyName}</Td>
                  <Td>
                    <PolicyTypeBadge type={product.policyType} />
                  </Td>
                  <Td>{formatTerm(product.termMonths)}</Td>
                  <Td align="right">{formatMoneyWhole(product.sumInsured)}</Td>
                  <Td>{formatFrequency(product.premiumFrequency)}</Td>
                  <Td align="right">{formatMoney(product.premiumAmount)}</Td>
                  <Td align="right">
                    <ButtonLink to={`/register?productId=${product.productId}`} size="sm" icon={ArrowRight}>
                      Register
                    </ButtonLink>
                  </Td>
                </Tr>
              ))}
            </Tbody>
          </Table>
        )}
      </Card>
    </>
  )
}
