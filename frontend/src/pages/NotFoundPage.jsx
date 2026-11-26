import { Compass, Home } from 'lucide-react'
import { ButtonLink } from '../components/ui/Button.jsx'

export default function NotFoundPage() {
  return (
    <div className="grid min-h-screen place-items-center px-4">
      <div className="text-center">
        <span className="mx-auto grid size-16 place-items-center rounded-2xl bg-brand-50 text-brand-600">
          <Compass className="size-8" aria-hidden="true" />
        </span>
        <p className="mt-6 text-sm font-semibold text-brand-700">404</p>
        <h1 className="mt-1 text-3xl font-bold tracking-tight text-slate-900">Page not found</h1>
        <p className="mt-2 text-slate-500">The page you are looking for doesn&apos;t exist.</p>
        <ButtonLink to="/dashboard" icon={Home} className="mt-8">
          Back to dashboard
        </ButtonLink>
      </div>
    </div>
  )
}
