import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from './AuthContext.jsx'

// Guards every child route. Remembers where the user was going so login can send them back there.
// This only improves the UX: the real protection is the backend rejecting requests without a valid JWT.
export default function ProtectedRoute() {
  const { isAuthenticated } = useAuth()
  const location = useLocation()

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }
  return <Outlet />
}
