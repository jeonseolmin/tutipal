import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider, RequireAuth } from './auth'
import AppShell from './components/AppShell'
import AppLoginDonePage from './pages/AppLoginDonePage'
import ChatPage from './pages/ChatPage'
import CreateSessionPage from './pages/CreateSessionPage'
import JoinSessionPage from './pages/JoinSessionPage'
import SessionPage from './pages/SessionPage'
import HomePage from './pages/HomePage'
import LoginPage from './pages/LoginPage'
import OverlayPage from './pages/OverlayPage'
import ReviewPage from './pages/ReviewPage'
import ScreenConsentPage from './pages/ScreenConsentPage'
import SettingsPage from './pages/SettingsPage'
import WidgetPage from './pages/WidgetPage'

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/widget" element={<WidgetPage />} />
          <Route path="/overlay" element={<OverlayPage />} />
          <Route path="/auth/app/done" element={<AppLoginDonePage />} />
          <Route element={<RequireAuth />}>
            <Route element={<AppShell />}>
              <Route index element={<HomePage />} />
              <Route path="chat" element={<ChatPage />} />
              <Route path="sessions/new" element={<CreateSessionPage />} />
              <Route path="sessions/join" element={<JoinSessionPage />} />
              <Route path="sessions/:id" element={<SessionPage />} />
              <Route path="screen-consent" element={<ScreenConsentPage />} />
              <Route path="review" element={<ReviewPage />} />
              <Route path="settings" element={<SettingsPage />} />
            </Route>
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
