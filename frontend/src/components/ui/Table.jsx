// Small table primitives so every table in the app looks the same.
// The wrapper scrolls sideways on small screens instead of squashing columns.
export function Table({ children, className = '' }) {
  return (
    <div className={`overflow-x-auto ${className}`}>
      <table className="min-w-full divide-y divide-slate-100 text-sm">{children}</table>
    </div>
  )
}

export function Th({ children, align = 'left', className = '' }) {
  const alignment = align === 'right' ? 'text-right' : align === 'center' ? 'text-center' : 'text-left'
  return (
    <th
      scope="col"
      className={`whitespace-nowrap bg-slate-50/80 px-3 py-3 text-xs first:pl-6 last:pr-6 font-semibold uppercase tracking-wide text-slate-500 ${alignment} ${className}`}
    >
      {children}
    </th>
  )
}

export function Td({ children, align = 'left', className = '' }) {
  const alignment = align === 'right' ? 'text-right tabular-nums' : align === 'center' ? 'text-center' : 'text-left'
  // first/last cells line up with the px-6 padding of the card header
  return (
    <td className={`whitespace-nowrap px-3 py-3.5 text-slate-700 first:pl-6 last:pr-6 ${alignment} ${className}`}>
      {children}
    </td>
  )
}

export function Tbody({ children }) {
  return <tbody className="divide-y divide-slate-100 bg-white">{children}</tbody>
}

export function Tr({ children, className = '' }) {
  return <tr className={`transition-colors hover:bg-slate-50/70 ${className}`}>{children}</tr>
}
