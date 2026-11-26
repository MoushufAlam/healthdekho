import { CalendarCheck, Eye, EyeOff, LogIn, ShieldCheck, Sparkles } from 'lucide-react'
import { useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router'
import { toApiError } from '../api/errors.js'
import { useAuth } from '../auth/AuthContext.jsx'
import Logo from '../components/layout/Logo.jsx'
import Alert from '../components/ui/Alert.jsx'
import Button from '../components/ui/Button.jsx'
import { Field, inputClasses } from '../components/ui/Field.jsx'

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

// seeded demo customers (V2 migration); only shown by the dev server, never in a production build
const DEMO_ACCOUNTS = [
  { name: 'Aarav Sharma', email: 'aarav.sharma@example.com', note: 'has a Term policy' },
  { name: 'Meera Iyer', email: 'meera.iyer@example.com', note: 'has a Health policy' },
  { name: 'Rohan Verma', email: 'rohan.verma@example.com', note: 'no policies yet' },
]
const DEMO_PASSWORD = 'Password@123'

const FEATURES = [
  { icon: Sparkles, title: 'AI recommendations', text: 'Personalised suggestions from our real product catalogue.' },
  { icon: ShieldCheck, title: 'Register in minutes', text: 'Your details are pre-filled and the premium is calculated instantly.' },
  { icon: CalendarCheck, title: 'Every policy in one place', text: 'Terms, maturity dates and premiums at a glance.' },
]

// Client-side checks give instant feedback; the server repeats them anyway (@Valid on LoginRequest)
function validate({ email, password }) {
  const errors = {}
  if (!email.trim()) errors.email = 'Email is required'
  else if (!EMAIL_PATTERN.test(email.trim())) errors.email = 'Enter a valid email address'
  if (!password) errors.password = 'Password is required'
  return errors
}

export default function LoginPage() {
  const { login, isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [form, setForm] = useState({ email: '', password: '' })
  const [fieldErrors, setFieldErrors] = useState({})
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)
  const [showPassword, setShowPassword] = useState(false)

  const notice = location.state?.notice
  const redirectTo = location.state?.from?.pathname ?? '/dashboard'

  if (isAuthenticated) {
    return <Navigate to={redirectTo} replace />
  }

  function updateField(event) {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
    setFieldErrors((current) => ({ ...current, [name]: undefined }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError(null)

    const errors = validate(form)
    setFieldErrors(errors)
    if (Object.keys(errors).length > 0) return

    setSubmitting(true)
    try {
      await login(form.email.trim(), form.password)
      navigate(redirectTo, { replace: true }) // FR-1A: go to the dashboard after a successful login
    } catch (err) {
      const apiError = toApiError(err)
      setError(apiError.message) // 401 -> "Invalid email or password"
      setFieldErrors(apiError.fieldErrors)
      setSubmitting(false)
    }
  }

  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      {/* brand panel (large screens only) */}
      <aside className="relative hidden overflow-hidden bg-linear-to-br from-brand-700 via-brand-800 to-brand-950 p-12 text-white lg:flex lg:flex-col">
        <div className="absolute -right-24 -top-24 size-96 rounded-full bg-brand-500/20 blur-3xl" aria-hidden="true" />
        <div className="absolute -bottom-32 -left-16 size-96 rounded-full bg-cyan-400/10 blur-3xl" aria-hidden="true" />

        <Logo light />

        <div className="relative my-auto max-w-lg">
          <h1 className="text-4xl font-extrabold leading-tight tracking-tight xl:text-5xl">
            Insurance that fits <span className="text-brand-200">your life.</span>
          </h1>
          <p className="mt-4 text-lg text-brand-100/90">
            View your policies, discover the right cover with AI and register in a few clicks.
          </p>

          <ul className="mt-10 space-y-5">
            {FEATURES.map(({ icon: Icon, title, text }) => (
              <li key={title} className="flex gap-4">
                <span className="grid size-11 shrink-0 place-items-center rounded-xl bg-white/10 ring-1 ring-white/20">
                  <Icon className="size-5 text-brand-200" aria-hidden="true" />
                </span>
                <div>
                  <p className="font-semibold">{title}</p>
                  <p className="text-sm text-brand-100/80">{text}</p>
                </div>
              </li>
            ))}
          </ul>
        </div>

        <p className="relative text-sm text-brand-200/70">Secured with Spring Security and JWT.</p>
      </aside>

      {/* login form */}
      <main className="flex items-center justify-center px-4 py-12 sm:px-8">
        <div className="w-full max-w-md animate-fade-in">
          <div className="mb-8 lg:hidden">
            <Logo />
          </div>

          <h2 className="text-3xl font-bold tracking-tight text-slate-900">Welcome back</h2>
          <p className="mt-2 text-slate-500">Log in to manage your insurance policies.</p>

          <div className="mt-8 space-y-4">
            {notice && <Alert tone="info">{notice}</Alert>}
            {error && (
              <Alert tone="error" title="Login failed" onClose={() => setError(null)}>
                {error}
              </Alert>
            )}
          </div>

          <form onSubmit={handleSubmit} noValidate className="mt-6 space-y-5">
            <Field label="Email address" htmlFor="email" required error={fieldErrors.email}>
              <input
                id="email"
                name="email"
                type="email"
                autoComplete="email"
                placeholder="you@example.com"
                value={form.email}
                onChange={updateField}
                aria-invalid={Boolean(fieldErrors.email)}
                aria-describedby="email-message"
                className={inputClasses(Boolean(fieldErrors.email))}
              />
            </Field>

            <Field label="Password" htmlFor="password" required error={fieldErrors.password}>
              <div className="relative">
                <input
                  id="password"
                  name="password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  placeholder="••••••••"
                  value={form.password}
                  onChange={updateField}
                  aria-invalid={Boolean(fieldErrors.password)}
                  aria-describedby="password-message"
                  className={`${inputClasses(Boolean(fieldErrors.password))} pr-11`}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((shown) => !shown)}
                  className="absolute inset-y-0 right-0 grid w-11 place-items-center text-slate-400 hover:text-slate-600"
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
                </button>
              </div>
            </Field>

            <Button type="submit" size="lg" icon={LogIn} loading={submitting} className="w-full">
              {submitting ? 'Logging in…' : 'Log in'}
            </Button>
          </form>

          {import.meta.env.DEV && (
            <div className="mt-10 rounded-2xl border border-dashed border-slate-300 p-4">
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                Demo accounts · password <span className="font-mono normal-case">{DEMO_PASSWORD}</span>
              </p>
              <div className="mt-3 grid gap-2">
                {DEMO_ACCOUNTS.map((account) => (
                  <button
                    key={account.email}
                    type="button"
                    onClick={() => {
                      setForm({ email: account.email, password: DEMO_PASSWORD })
                      setFieldErrors({})
                      setError(null)
                    }}
                    className="flex items-center justify-between rounded-xl px-3 py-2 text-left text-sm transition hover:bg-slate-100"
                  >
                    <span className="font-medium text-slate-800">{account.name}</span>
                    <span className="text-xs text-slate-500">{account.note}</span>
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>
      </main>
    </div>
  )
}
