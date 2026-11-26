import { Link } from 'react-router'
import Spinner from './Spinner.jsx'

const BASE =
  'inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-xl font-semibold transition ' +
  'focus-visible:outline-none focus-visible:ring-4 disabled:cursor-not-allowed disabled:opacity-60'

const VARIANTS = {
  primary: 'bg-brand-600 text-white shadow-sm hover:bg-brand-700 focus-visible:ring-brand-200',
  secondary: 'bg-white text-slate-700 ring-1 ring-inset ring-slate-200 hover:bg-slate-50 focus-visible:ring-slate-200',
  ghost: 'text-slate-600 hover:bg-slate-100 hover:text-slate-900 focus-visible:ring-slate-200',
  ai: 'bg-linear-to-r from-violet-600 to-indigo-600 text-white shadow-sm hover:from-violet-700 hover:to-indigo-700 focus-visible:ring-violet-200',
  white: 'bg-white text-slate-900 shadow-sm hover:bg-white/90 focus-visible:ring-white/40',
}

const SIZES = {
  sm: 'h-8 px-3 text-xs',
  md: 'h-10 px-4 text-sm',
  lg: 'h-12 px-6 text-base',
}

function classes(variant, size, className) {
  return `${BASE} ${VARIANTS[variant]} ${SIZES[size]} ${className}`
}

export default function Button({
  variant = 'primary',
  size = 'md',
  type = 'button',
  loading = false,
  disabled = false,
  icon: Icon,
  className = '',
  children,
  ...props
}) {
  return (
    <button type={type} disabled={disabled || loading} className={classes(variant, size, className)} {...props}>
      {loading ? <Spinner className="size-4" /> : Icon && <Icon className="size-4" aria-hidden="true" />}
      {children}
    </button>
  )
}

// Same look as Button, but navigates (renders an <a> through React Router)
export function ButtonLink({ to, state, variant = 'primary', size = 'md', icon: Icon, className = '', children, ...props }) {
  return (
    <Link to={to} state={state} className={classes(variant, size, className)} {...props}>
      {Icon && <Icon className="size-4" aria-hidden="true" />}
      {children}
    </Link>
  )
}
