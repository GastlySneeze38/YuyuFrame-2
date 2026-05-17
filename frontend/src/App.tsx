import { useEffect } from 'react'
import { Navigate, Route, Routes, useNavigate, useLocation } from 'react-router-dom'
import { TitleBar } from '@/components/TitleBar'
import Login from '@/pages/Login'
import Home from '@/pages/Home'
import Mods from '@/pages/Mods'
import Settings from '@/pages/Settings'
import YuyuLogin from '@/pages/YuyuLogin'
import { useStore } from '@/stores/useStore'
import { setApiToken } from '@/api/client'

function AuthGuard({ children }: { children: React.ReactNode }) {
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const { yuyuToken } = useStore()

  useEffect(() => {
    if (!yuyuToken && pathname !== '/yuyu') {
      navigate('/yuyu', { replace: true })
    }
  }, [yuyuToken, pathname])

  return <>{children}</>
}

export default function App() {
  const { theme, brightness, yuyuToken } = useStore()

  // Re-sync the API token on app mount (store is persisted but token is NOT,
  // so this is always null after restart — forces /yuyu login)
  useEffect(() => {
    setApiToken(yuyuToken)
  }, [yuyuToken])

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme)
  }, [theme])

  return (
    <div className="flex h-screen flex-col overflow-hidden bg-bg-primary">
      <TitleBar />
      <div className="flex-1 overflow-hidden" style={{ filter: `brightness(${brightness / 100})` }}>
        <Routes>
          {/* YuyuFrame account gate — always accessible */}
          <Route path="/yuyu" element={<YuyuLogin />} />

          {/* Protected routes */}
          <Route
            path="/*"
            element={
              <AuthGuard>
                <Routes>
                  <Route path="/" element={<Navigate to="/home" replace />} />
                  <Route path="/home" element={<Home />} />
                  <Route path="/login" element={<Login />} />
                  <Route path="/mods" element={<Mods />} />
                  <Route path="/settings" element={<Settings />} />
                </Routes>
              </AuthGuard>
            }
          />
        </Routes>
      </div>
    </div>
  )
}
