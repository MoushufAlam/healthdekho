const TONES = {
  teal: 'bg-brand-50 text-brand-700',
  violet: 'bg-violet-50 text-violet-700',
  amber: 'bg-amber-50 text-amber-700',
  sky: 'bg-sky-50 text-sky-700',
  rose: 'bg-rose-50 text-rose-700',
}

export default function StatCard({ icon: Icon, label, value, hint, tone = 'teal' }) {
  return (
    <div className="rounded-2xl bg-white p-5 shadow-card ring-1 ring-slate-200/70">
      <div className="flex items-center gap-4">
        <span className={`grid size-11 shrink-0 place-items-center rounded-xl ${TONES[tone]}`}>
          <Icon className="size-5" aria-hidden="true" />
        </span>
        <div className="min-w-0">
          <p className="text-sm font-medium text-slate-500">{label}</p>
          <p className="truncate text-xl font-bold tracking-tight text-slate-900">{value}</p>
        </div>
      </div>
      {hint && <p className="mt-3 text-xs text-slate-500">{hint}</p>}
    </div>
  )
}
