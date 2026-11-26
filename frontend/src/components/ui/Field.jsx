import { Lock } from 'lucide-react'

// Shared look for <input>, <select> and <textarea>; turns red when the field has an error
export function inputClasses(hasError = false) {
  const state = hasError
    ? 'ring-rose-300 focus:ring-rose-500 bg-rose-50/40'
    : 'ring-slate-200 focus:ring-brand-500 bg-white'
  return (
    'block w-full rounded-xl border-0 px-3.5 py-2.5 text-sm text-slate-900 shadow-xs ring-1 ring-inset ' +
    'placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-inset ' +
    `disabled:cursor-not-allowed disabled:bg-slate-50 disabled:text-slate-500 ${state}`
  )
}

// Label + control + hint/error. Pass the input as children so any control type works.
export function Field({ label, htmlFor, required = false, hint, error, children, className = '' }) {
  const messageId = htmlFor ? `${htmlFor}-message` : undefined
  return (
    <div className={className}>
      <label htmlFor={htmlFor} className="mb-1.5 block text-sm font-medium text-slate-700">
        {label}
        {required && <span className="ml-0.5 text-rose-500">*</span>}
      </label>
      {children}
      {error ? (
        <p id={messageId} className="mt-1.5 text-xs font-medium text-rose-600">
          {error}
        </p>
      ) : (
        hint && (
          <p id={messageId} className="mt-1.5 text-xs text-slate-500">
            {hint}
          </p>
        )
      )}
    </div>
  )
}

// A value the user can see but not change (auto-filled from the profile or the product)
export function ReadOnlyField({ label, value, emphasis = false }) {
  return (
    <div>
      <p className="mb-1.5 flex items-center gap-1 text-sm font-medium text-slate-500">
        {label}
        <Lock className="size-3 text-slate-400" aria-label="auto-filled" />
      </p>
      <div
        className={`rounded-xl bg-slate-50 px-3.5 py-2.5 text-sm ring-1 ring-inset ring-slate-100 ${
          emphasis ? 'font-semibold text-slate-900' : 'text-slate-700'
        }`}
      >
        {value ?? '—'}
      </div>
    </div>
  )
}
