import { ShieldPlus } from 'lucide-react'

export default function Logo({ light = false }) {
  return (
    <span className="inline-flex items-center gap-2.5">
      <span className="grid size-9 place-items-center rounded-xl bg-linear-to-br from-brand-400 to-brand-700 text-white shadow-sm">
        <ShieldPlus className="size-5" aria-hidden="true" />
      </span>
      <span className={`text-lg font-extrabold tracking-tight ${light ? 'text-white' : 'text-slate-900'}`}>
        Health<span className={light ? 'text-brand-200' : 'text-brand-600'}>Dekho</span>
      </span>
    </span>
  )
}
