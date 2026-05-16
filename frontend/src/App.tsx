import { useEffect } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { TitleBar } from '@/components/TitleBar'
import Login from '@/pages/Login'
import Home from '@/pages/Home'
import { useStore } from '@/stores/useStore'

export default function App() {
  const { theme } = useStore()

  // Apply theme on mount and whenever it changes
  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme)
  }, [theme])

  return (
    <div className="flex h-screen flex-col overflow-hidden bg-bg-primary">
      <TitleBar />
      <div className="flex-1 overflow-hidden">
        <Routes>
          <Route path="/" element={<Navigate to="/login" replace />} />
          <Route path="/login" element={<Login />} />
          <Route path="/home" element={<Home />} />
        </Routes>
      </div>
    </div>
  )
}
