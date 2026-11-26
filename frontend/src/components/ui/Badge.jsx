import { HeartPulse, PiggyBank, ShieldCheck, Umbrella } from 'lucide-react'
import { formatEnum } from '../../lib/format.js'

const TONES = {
  green: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20',
  blue: 'bg-sky-50 text-sky-700 ring-sky-600/20',
  amber: 'bg-amber-50 text-amber-800 ring-amber-600/25',
  red: 'bg-rose-50 text-rose-700 ring-rose-600/20',
  violet: 'bg-violet-50 text-violet-700 ring-violet-600/20',
  teal: 'bg-brand-50 text-brand-800 ring-brand-600/20',
  gray: 'bg-slate-100 text-slate-600 ring-slate-500/20',
}

const DOTS = {
  green: 'bg-emerald-500',
  blue: 'bg-sky-500',
  amber: 'bg-amber-500',
  red: 'bg-rose-500',
  violet: 'bg-violet-500',
  teal: 'bg-brand-500',
  gray: 'bg-slate-400',
}

export function Badge({ tone = 'gray', dot = false, icon: Icon, className = '', children }) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 whitespace-nowrap rounded-full px-2.5 py-0.5 text-xs font-semibold ring-1 ring-inset ${TONES[tone]} ${className}`}
    >
      {dot && <span className={`size-1.5 rounded-full ${DOTS[tone]}`} aria-hidden="true" />}
      {Icon && <Icon className="size-3.5" aria-hidden="true" />}
      {children}
    </span>
  )
}

// policy, installment, search and suitability statuses all share one colour scheme
const STATUS_TONES = {
  ACTIVE: 'green',
  FULLY_PAID: 'blue',
  LAPSED: 'amber',
  MATURED: 'violet',
  CANCELLED: 'gray',
  PAID: 'green',
  PENDING: 'gray',
  PARTIALLY_PAID: 'amber',
  OVERDUE: 'red',
  ON_TRACK: 'green',
  HIGH: 'green',
  MEDIUM: 'amber',
  LOW: 'gray',
}

export function StatusBadge({ status }) {
  return (
    <Badge tone={STATUS_TONES[status] ?? 'gray'} dot>
      {formatEnum(status)}
    </Badge>
  )
}

const POLICY_TYPES = {
  TERM: { icon: ShieldCheck, tone: 'blue' },
  HEALTH: { icon: HeartPulse, tone: 'red' },
  SAVINGS: { icon: PiggyBank, tone: 'amber' },
  LIFE: { icon: Umbrella, tone: 'violet' },
}

export function PolicyTypeBadge({ type }) {
  const { icon, tone } = POLICY_TYPES[type] ?? { tone: 'gray' }
  return (
    <Badge tone={tone} icon={icon}>
      {formatEnum(type)}
    </Badge>
  )
}
