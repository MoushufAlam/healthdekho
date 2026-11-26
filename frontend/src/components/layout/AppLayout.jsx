import { FilePlus2, LayoutDashboard, LogOut, Search, Sparkles, Wallet } from 'lucide-react'
import { Link, NavLink, Outlet } from 'react-router'
import { useAuth } from '../../auth/AuthContext.jsx'
import { initials } from '../../lib/format.js'
import Logo from './Logo.jsx'

const NAV_ITEMS = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/recommendations', label: 'AI Advisor', icon: Sparkles },
  { to: '/register', label: 'New Policy', icon: FilePlus2 },
  // OPTIONAL: FR-4 / FR-5 (not evaluated) - remove these two entries together with their routes
  { to: '/payments', label: 'Pay Premium', icon: Wallet },
  { to: '/search', label: 'Search', icon: Search },
]

function navLinkClasses({ isActive }) {
  return (
    'inline-flex items-center gap-2 whitespace-nowrap rounded-lg px-3 py-2 text-sm font-medium transition ' +
    (isActive ? 'bg-brand-50 text-brand-800' : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900')
  )
}

// Shell for every logged-in page: header + navigation, then the current page in <Outlet />
export default function AppLayout() {
  const { customer, logout } = useAuth()

  return (
    <div className="flex min-h-screen flex-col">
      <header className="sticky top-0 z-30 border-b border-slate-200/80 bg-white/85 backdrop-blur-md">
        <div className="mx-auto flex h-16 max-w-7xl items-center gap-6 px-4 sm:px-6 lg:px-8">
          <Link to="/dashboard" aria-label="HealthDekho home">
            <Logo />
          </Link>

          <nav className="hidden flex-1 items-center gap-1 lg:flex" aria-label="Main">
            {NAV_ITEMS.map(({ to, label, icon: Icon }) => (
              <NavLink key={to} to={to} className={navLinkClasses}>
                <Icon className="size-4" aria-hidden="true" />
                {label}
              </NavLink>
            ))}
          </nav>

          <div className="ml-auto flex items-center gap-3">
            <div className="hidden text-right sm:block">
              <p className="text-sm font-semibold leading-tight text-slate-900">{customer?.fullName}</p>
              <p className="text-xs leading-tight text-slate-500">{customer?.email}</p>
            </div>
            <span
              className="grid size-9 place-items-center rounded-full bg-linear-to-br from-brand-500 to-brand-700 text-sm font-bold text-white"
              aria-hidden="true"
            >
              {initials(customer?.fullName)}
            </span>
            <button
              type="button"
              onClick={() => logout()}
              className="inline-flex items-center gap-1.5 rounded-lg px-2.5 py-2 text-sm font-medium text-slate-500 transition hover:bg-rose-50 hover:text-rose-600"
            >
              <LogOut className="size-4" aria-hidden="true" />
              <span className="hidden sm:inline">Logout</span>
            </button>
          </div>
        </div>

        {/* below lg the nav moves to its own scrollable row */}
        <nav className="flex gap-1 overflow-x-auto border-t border-slate-100 px-4 py-2 lg:hidden" aria-label="Main">
          {NAV_ITEMS.map(({ to, label, icon: Icon }) => (
            <NavLink key={to} to={to} className={navLinkClasses}>
              <Icon className="size-4" aria-hidden="true" />
              {label}
            </NavLink>
          ))}
        </nav>
      </header>

      <main className="mx-auto w-full max-w-7xl flex-1 animate-fade-in px-4 py-8 sm:px-6 lg:px-8">
        <Outlet />
      </main>

      <footer className="border-t border-slate-200/80 bg-white">
        <div className="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-2 px-4 py-5 text-xs text-slate-500 sm:px-6 lg:px-8">
          <p>© {new Date().getFullYear()} HealthDekho Insurance. Capstone project.</p>
          <p>Your data is only visible to you.</p>
        </div>
      </footer>
    </div>
  )
}
