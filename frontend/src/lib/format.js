// Display helpers. The API sends money as numbers (1250.00), dates as "yyyy-MM-dd" and enums as "HALF_YEARLY".

const rupees = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

const rupeesWhole = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 0,
})

export function formatMoney(value) {
  return value == null ? '—' : rupees.format(Number(value))
}

// for big round numbers like sum insured: ₹10,00,000
export function formatMoneyWhole(value) {
  return value == null ? '—' : rupeesWhole.format(Number(value))
}

// new Date("2026-12-01") would be read as UTC midnight and can show the previous day in some
// time zones, so build the date from its parts instead
export function parseIsoDate(iso) {
  const [year, month, day] = iso.split('-').map(Number)
  return new Date(year, month - 1, day)
}

export function formatDate(iso) {
  if (!iso) return '—'
  return parseIsoDate(iso).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })
}

// "2026-12" -> "Dec 2026"
export function formatPeriod(yearMonth) {
  if (!yearMonth) return '—'
  return formatDate(`${yearMonth}-01`).split(' ').slice(1).join(' ')
}

export function formatTerm(months) {
  if (months == null) return '—'
  if (months % 12 === 0) {
    const years = months / 12
    return `${years} ${years === 1 ? 'year' : 'years'}`
  }
  return `${months} months`
}

// today as "yyyy-MM-dd" in the user's local time zone ("en-CA" happens to use that format)
export function todayIso() {
  return new Date().toLocaleDateString('en-CA')
}

const FREQUENCY_LABELS = { MONTHLY: 'Monthly', QUARTERLY: 'Quarterly', HALF_YEARLY: 'Half-Yearly', YEARLY: 'Yearly' }

export function formatFrequency(value) {
  return FREQUENCY_LABELS[value] ?? formatEnum(value)
}

// "PARTIALLY_PAID" -> "Partially paid"
export function formatEnum(value) {
  if (!value) return '—'
  const words = value.toLowerCase().replaceAll('_', ' ')
  return words.charAt(0).toUpperCase() + words.slice(1)
}

export function firstName(fullName) {
  return fullName?.split(' ')[0] ?? ''
}

export function initials(fullName) {
  return (fullName ?? '?')
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0].toUpperCase())
    .join('')
}
