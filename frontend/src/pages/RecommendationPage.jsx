import { ArrowRight, Bot, Cpu, RotateCcw, Sparkles, Wand2 } from 'lucide-react'
import { useRef, useState } from 'react'
import { toApiError } from '../api/errors.js'
import { getRecommendations } from '../api/recommendationApi.js'
import { useAuth } from '../auth/AuthContext.jsx'
import Alert from '../components/ui/Alert.jsx'
import { Badge, PolicyTypeBadge, StatusBadge } from '../components/ui/Badge.jsx'
import Button, { ButtonLink } from '../components/ui/Button.jsx'
import { Card, CardBody, CardHeader } from '../components/ui/Card.jsx'
import EmptyState from '../components/ui/EmptyState.jsx'
import { Field, inputClasses } from '../components/ui/Field.jsx'
import PageHeader from '../components/ui/PageHeader.jsx'
import Spinner from '../components/ui/Spinner.jsx'
import { Table, Tbody, Td, Th, Tr } from '../components/ui/Table.jsx'
import { formatDate, formatFrequency, formatMoney, formatMoneyWhole, formatTerm, todayIso } from '../lib/format.js'

const EMPTY_FORM = { dateOfBirth: '', preferredPolicyType: '', preferredTerm: '', insuranceGoals: '' }
const GOALS_MAX = 1000
const TERM_SUGGESTIONS = ['1 year', '5 years', '10 years', '20 years']
const GOAL_EXAMPLES = [
  'Health cover for my parents',
  'Protect my family if something happens to me',
  'Save regularly for my child’s education',
]

// mirrors the server rules (RecommendationRequest.hasAnyInput + @Size) for instant feedback
function validate(form) {
  const errors = {}
  const anyInput = Object.values(form).some((value) => value.trim() !== '')
  if (!anyInput) errors.form = 'Fill in at least one field so we can personalise the recommendations.'
  if (form.dateOfBirth && form.dateOfBirth > todayIso()) errors.dateOfBirth = 'Date of birth cannot be in the future'
  if (form.insuranceGoals.length > GOALS_MAX) errors.insuranceGoals = `At most ${GOALS_MAX} characters`
  return errors
}

// FR-2: AI insurance recommendations
export default function RecommendationPage() {
  const { customer } = useAuth()
  const [form, setForm] = useState(EMPTY_FORM)
  const [fieldErrors, setFieldErrors] = useState({})
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)
  const [result, setResult] = useState(null)
  const resultsRef = useRef(null)

  function updateField(event) {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
    setFieldErrors((current) => ({ ...current, [name]: undefined, form: undefined }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError(null)
    const errors = validate(form)
    setFieldErrors(errors)
    if (Object.keys(errors).length > 0) return

    setSubmitting(true)
    setResult(null)
    try {
      const response = await getRecommendations(form)
      setResult(response)
      // wait one frame so the results exist before scrolling to them
      requestAnimationFrame(() => resultsRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' }))
    } catch (err) {
      const apiError = toApiError(err)
      setError(apiError.message)
      setFieldErrors(apiError.fieldErrors)
    } finally {
      setSubmitting(false)
    }
  }

  function reset() {
    setForm(EMPTY_FORM)
    setFieldErrors({})
    setError(null)
    setResult(null)
  }

  return (
    <>
      <PageHeader
        eyebrow="AI Insurance Advisor"
        title="Find the right cover for you"
        description="Share whatever you like (even one field is enough). We only recommend real products from our catalogue that you don't already own."
      />

      <Card className="mb-8">
        <CardHeader icon={Wand2} title="Tell us about yourself" subtitle="All fields are optional, but fill in at least one" />
        <CardBody>
          <form onSubmit={handleSubmit} noValidate className="space-y-6">
            {fieldErrors.form && <Alert tone="warning">{fieldErrors.form}</Alert>}
            {error && (
              <Alert tone="error" title="Could not get recommendations" onClose={() => setError(null)}>
                {error}
              </Alert>
            )}

            <div className="grid gap-5 md:grid-cols-3">
              <Field
                label="Date of birth"
                htmlFor="dateOfBirth"
                error={fieldErrors.dateOfBirth}
                hint={customer?.dateOfBirth ? `Leave empty to use your profile (${formatDate(customer.dateOfBirth)})` : undefined}
              >
                <input
                  id="dateOfBirth"
                  name="dateOfBirth"
                  type="date"
                  max={todayIso()}
                  value={form.dateOfBirth}
                  onChange={updateField}
                  className={inputClasses(Boolean(fieldErrors.dateOfBirth))}
                />
              </Field>

              <Field label="Insurance type" htmlFor="preferredPolicyType" error={fieldErrors.preferredPolicyType}>
                <select
                  id="preferredPolicyType"
                  name="preferredPolicyType"
                  value={form.preferredPolicyType}
                  onChange={updateField}
                  className={inputClasses(Boolean(fieldErrors.preferredPolicyType))}
                >
                  <option value="">Any type</option>
                  <option value="TERM">Term</option>
                  <option value="HEALTH">Health</option>
                  <option value="SAVINGS">Savings</option>
                  <option value="LIFE">Life</option>
                </select>
              </Field>

              <Field label="Preferred policy term" htmlFor="preferredTerm" error={fieldErrors.preferredTerm} hint="e.g. 1 year, 10 years">
                <input
                  id="preferredTerm"
                  name="preferredTerm"
                  type="text"
                  list="term-suggestions"
                  maxLength={50}
                  placeholder="Any term"
                  value={form.preferredTerm}
                  onChange={updateField}
                  className={inputClasses(Boolean(fieldErrors.preferredTerm))}
                />
                <datalist id="term-suggestions">
                  {TERM_SUGGESTIONS.map((term) => (
                    <option key={term} value={term} />
                  ))}
                </datalist>
              </Field>
            </div>

            <Field label="Insurance goals and preferences" htmlFor="insuranceGoals" error={fieldErrors.insuranceGoals}>
              <textarea
                id="insuranceGoals"
                name="insuranceGoals"
                rows={3}
                maxLength={GOALS_MAX}
                placeholder="What do you want to protect or save for?"
                value={form.insuranceGoals}
                onChange={updateField}
                className={`${inputClasses(Boolean(fieldErrors.insuranceGoals))} resize-y`}
              />
              <div className="mt-2 flex flex-wrap items-center justify-between gap-2">
                <div className="flex flex-wrap gap-2">
                  {GOAL_EXAMPLES.map((example) => (
                    <button
                      key={example}
                      type="button"
                      onClick={() => setForm((current) => ({ ...current, insuranceGoals: example }))}
                      className="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600 transition hover:bg-brand-50 hover:text-brand-700"
                    >
                      {example}
                    </button>
                  ))}
                </div>
                <span className="text-xs text-slate-400">
                  {form.insuranceGoals.length}/{GOALS_MAX}
                </span>
              </div>
            </Field>

            <div className="flex flex-wrap gap-3 border-t border-slate-100 pt-5">
              <Button type="submit" variant="ai" size="lg" icon={Sparkles} loading={submitting}>
                {submitting ? 'Analysing…' : 'Get recommendations'}
              </Button>
              <Button variant="ghost" size="lg" icon={RotateCcw} onClick={reset} disabled={submitting}>
                Reset
              </Button>
            </div>
          </form>
        </CardBody>
      </Card>

      {submitting && (
        <Card>
          <div className="flex flex-col items-center gap-3 px-6 py-14 text-center">
            <Spinner className="size-9 text-violet-600" />
            <p className="font-semibold text-slate-900">Our advisor is analysing your profile…</p>
            <p className="text-sm text-slate-500">This can take up to 20 seconds.</p>
          </div>
        </Card>
      )}

      {result && (
        <div ref={resultsRef} className="scroll-mt-24">
          <RecommendationResults result={result} />
        </div>
      )}
    </>
  )
}

function RecommendationResults({ result }) {
  const fromAi = result.source === 'AI'

  return (
    <Card className="animate-fade-in">
      <CardHeader
        icon={fromAi ? Bot : Cpu}
        title="Recommended for you"
        subtitle={`Age used: ${result.calculatedAge} · Request #${result.recommendationRequestId}`}
        action={
          fromAi ? (
            <Badge tone="violet" icon={Sparkles}>
              AI powered · {result.modelName}
            </Badge>
          ) : (
            <Badge tone="gray" icon={Cpu}>
              Rule-based suggestions
            </Badge>
          )
        }
      />

      {result.message && (
        <div className="px-6 pt-5">
          <Alert tone={fromAi ? 'info' : 'warning'}>{result.message}</Alert>
        </div>
      )}

      {result.recommendations.length === 0 ? (
        <EmptyState
          icon={Sparkles}
          title="No matching products"
          description="There are no eligible products left for you right now. Try different preferences."
        />
      ) : (
        <Table className="mt-4">
          <thead>
            <tr>
              <Th>Product code</Th>
              <Th>Policy name</Th>
              <Th>Type</Th>
              <Th>Term</Th>
              <Th align="right">Sum insured</Th>
              <Th>Frequency</Th>
              <Th align="right">Premium</Th>
              <Th>Why we recommend it</Th>
              <Th>Suitability</Th>
              <Th align="right">Action</Th>
            </tr>
          </thead>
          <Tbody>
            {result.recommendations.map((item) => (
              <Tr key={item.recommendationItemId}>
                <Td>
                  <span className="inline-flex items-center gap-2.5">
                    <span
                      className="grid size-6 place-items-center rounded-full bg-violet-50 text-xs font-bold text-violet-700"
                      title={`Rank ${item.rank}`}
                    >
                      {item.rank}
                    </span>
                    <span className="font-mono text-xs font-semibold text-slate-900">{item.productCode}</span>
                  </span>
                </Td>
                <Td className="min-w-36 whitespace-normal! font-medium text-slate-900">{item.policyName}</Td>
                <Td>
                  <PolicyTypeBadge type={item.policyType} />
                </Td>
                <Td>{formatTerm(item.termMonths)}</Td>
                <Td align="right">{formatMoneyWhole(item.sumInsured)}</Td>
                <Td>{formatFrequency(item.premiumFrequency)}</Td>
                <Td align="right">{formatMoney(item.premiumAmount)}</Td>
                <Td className="min-w-52 max-w-xs whitespace-normal! text-slate-600">{item.recommendationReason}</Td>
                <Td>
                  <StatusBadge status={item.suitability} />
                </Td>
                <Td align="right">
                  {/* the item id travels to registration so the server can mark this suggestion as chosen */}
                  <ButtonLink
                    to={`/register?productId=${item.productId}&recommendationItemId=${item.recommendationItemId}`}
                    size="sm"
                    icon={ArrowRight}
                  >
                    Register
                  </ButtonLink>
                </Td>
              </Tr>
            ))}
          </Tbody>
        </Table>
      )}
    </Card>
  )
}
