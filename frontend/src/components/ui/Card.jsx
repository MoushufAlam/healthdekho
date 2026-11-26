export function Card({ className = '', children, ...props }) {
  return (
    <section className={`overflow-hidden rounded-2xl bg-white shadow-card ring-1 ring-slate-200/70 ${className}`} {...props}>
      {children}
    </section>
  )
}

export function CardHeader({ title, subtitle, icon: Icon, action }) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-100 px-6 py-5">
      <div className="flex items-center gap-3">
        {Icon && (
          <span className="grid size-10 place-items-center rounded-xl bg-brand-50 text-brand-700">
            <Icon className="size-5" aria-hidden="true" />
          </span>
        )}
        <div>
          <h2 className="text-base font-semibold text-slate-900">{title}</h2>
          {subtitle && <p className="text-sm text-slate-500">{subtitle}</p>}
        </div>
      </div>
      {action}
    </div>
  )
}

export function CardBody({ className = '', children }) {
  return <div className={`px-6 py-5 ${className}`}>{children}</div>
}
