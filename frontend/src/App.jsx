import { Navigate, Route, Routes } from 'react-router'
import ProtectedRoute from './auth/ProtectedRoute.jsx'
import AppLayout from './components/layout/AppLayout.jsx'
import LoginPage from './pages/LoginPage.jsx'
import DashboardPage from './pages/DashboardPage.jsx'
import RecommendationPage from './pages/RecommendationPage.jsx'
import RegisterPolicyPage from './pages/RegisterPolicyPage.jsx'
import NotFoundPage from './pages/NotFoundPage.jsx'
// OPTIONAL: FR-4 / FR-5 (not evaluated) - delete these two imports and their routes to remove the features
import PaymentPage from './optional/fr4-payment/PaymentPage.jsx'
import SearchPage from './optional/fr5-search/SearchPage.jsx'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />

      {/* everything inside needs a logged-in customer */}
      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/recommendations" element={<RecommendationPage />} />
          <Route path="/register" element={<RegisterPolicyPage />} />

          {/* OPTIONAL: FR-4 Premium Payment */}
          <Route path="/payments" element={<PaymentPage />} />
          <Route path="/payments/:policyNumber" element={<PaymentPage />} />

          {/* OPTIONAL: FR-5 Policy Search */}
          <Route path="/search" element={<SearchPage />} />
        </Route>
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
