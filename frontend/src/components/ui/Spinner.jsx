import { LoaderCircle } from 'lucide-react'

export default function Spinner({ className = 'size-5' }) {
  return <LoaderCircle className={`animate-spin ${className}`} aria-hidden="true" />
}

export function PageLoader({ label = 'Loading…' }) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-24 text-slate-500" role="status">
      <Spinner className="size-8 text-brand-600" />
      <p className="text-sm font-medium">{label}</p>
    </div>
  )
}
