import { CircleAlert, CircleCheck, Info, TriangleAlert, X } from 'lucide-react'

const STYLES = {
  error: { icon: CircleAlert, box: 'bg-rose-50 text-rose-800 ring-rose-200', iconColor: 'text-rose-500' },
  success: { icon: CircleCheck, box: 'bg-emerald-50 text-emerald-800 ring-emerald-200', iconColor: 'text-emerald-500' },
  warning: { icon: TriangleAlert, box: 'bg-amber-50 text-amber-900 ring-amber-200', iconColor: 'text-amber-500' },
  info: { icon: Info, box: 'bg-sky-50 text-sky-800 ring-sky-200', iconColor: 'text-sky-500' },
}

export default function Alert({ tone = 'info', title, children, onClose, className = '' }) {
  const { icon: Icon, box, iconColor } = STYLES[tone]
  return (
    <div
      role={tone === 'error' ? 'alert' : 'status'}
      className={`flex animate-fade-in gap-3 rounded-xl px-4 py-3 text-sm ring-1 ring-inset ${box} ${className}`}
    >
      <Icon className={`mt-0.5 size-5 shrink-0 ${iconColor}`} aria-hidden="true" />
      <div className="min-w-0 flex-1">
        {title && <p className="font-semibold">{title}</p>}
        {children && <div className={title ? 'mt-0.5' : ''}>{children}</div>}
      </div>
      {onClose && (
        <button type="button" onClick={onClose} className="shrink-0 opacity-60 hover:opacity-100" aria-label="Dismiss">
          <X className="size-4" />
        </button>
      )}
    </div>
  )
}
