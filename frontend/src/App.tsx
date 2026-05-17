import { useEffect } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { TitleBar } from '@/components/TitleBar'
import Login from '@/pages/Login'
import Home from '@/pages/Home'
import Settings from '@/pages/Settings'
import { useStore } from '@/stores/useStore'

export default function App() {
  const { theme, brightness } = useStore()

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme)
  }, [theme])

  return (
    <div className="flex h-screen flex-col overflow-hidden bg-bg-primary">
      <TitleBar />
      <div className="flex-1 overflow-hidden" style={{ filter: `brightness(${brightness / 100})` }}>
        <Routes>
          <Route path="/" element={<Navigate to="/home" replace />} />
          <Route path="/login" element={<Login />} />
          <Route path="/home" element={<Home />} />
          <Route path="/settings" element={<Settings />} />
        </Routes>
      </div>
    </div>
  )
}
