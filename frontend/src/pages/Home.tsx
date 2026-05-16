import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '@/api/client'
import { useStore } from '@/stores/useStore'
import type { ProgressResponse } from '@/types'

export default function Home() {
  const navigate = useNavigate()
  const { username, clearUser, versions, setVersions, selectedVersion, setSelectedVersion, ram, setRam, gameRunning, setGameRunning, theme } =
    useStore()
  const [progress, setProgress] = useState<ProgressResponse | null>(null)
  const [launchMsg, setLaunchMsg] = useState('')
  const [showReleaseOnly, setShowReleaseOnly] = useState(true)
  const progressRef = useRef<ReturnType<typeof setInterval> | null>(null)

  // Load versions
  useEffect(() => {
    api.versions.list().then((v) => {
      setVersions(v)
      if (!selectedVersion && v.length > 0) {
        const latest = v.find((ver) => ver.version_type === 'release')
        setSelectedVersion(latest?.id ?? v[0].id)
      }
    }).catch(() => {})
  }, [])

  // Poll progress while launching
  useEffect(() => {
    if (gameRunning) {
      progressRef.current = setInterval(async () => {
        try {
          const p = await api.launch.progress()
          setProgress(p)
          if (!p.downloading && !p.message.includes('cours')) {
            setGameRunning(false)
            setProgress(null)
          }
        } catch {
          // ignore
        }
      }, 1000)
    } else {
      if (progressRef.current) {
        clearInterval(progressRef.current)
        progressRef.current = null
      }
    }
    return () => {
      if (progressRef.current) clearInterval(progressRef.current)
    }
  }, [gameRunning])

  const handleLogout = async () => {
    await api.auth.logout()
    clearUser()
    navigate('/login', { replace: true })
  }

  const handleLaunch = async () => {
    if (!selectedVersion || gameRunning) return
    setLaunchMsg('')
    try {
      const resp = await api.launch.start(selectedVersion, ram)
      if (resp.success) {
        setGameRunning(true)
      } else {
        setLaunchMsg(resp.message)
      }
    } catch (e) {
      setLaunchMsg(e instanceof Error ? e.message : 'Erreur de lancement')
    }
  }

  const filteredVersions = showReleaseOnly
    ? versions.filter((v) => v.version_type === 'release')
    : versions

  const isGamer = theme === 'gamer'

  return (
    <div className="flex h-full bg-bg-primary transition-theme">
      {/* Sidebar */}
      <aside className="flex w-16 flex-col items-center gap-4 border-r border-border bg-bg-secondary py-4 transition-theme">
        <NavIcon title="Jouer" active>
          <svg viewBox="0 0 24 24" fill="currentColor" className="h-5 w-5">
            <path d="M8 5v14l11-7z" />
          </svg>
        </NavIcon>
        <NavIcon title="Paramètres">
          <svg viewBox="0 0 24 24" fill="currentColor" className="h-5 w-5">
            <path d="M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z" />
          </svg>
        </NavIcon>
      </aside>

      {/* Main */}
      <main className="flex flex-1 flex-col">
        {/* Hero area */}
        <div
          className="relative flex flex-1 flex-col items-center justify-center gap-8"
          style={{
            background: isGamer
              ? 'radial-gradient(ellipse at 50% 40%, rgb(239 68 68 / 0.06) 0%, transparent 60%)'
              : 'radial-gradient(ellipse at 50% 40%, rgb(99 102 241 / 0.06) 0%, transparent 60%)',
          }}
        >
          {/* MC Logo placeholder */}
          <div className="flex flex-col items-center gap-2 select-none">
            <div className="font-black text-6xl tracking-tighter text-txt-primary opacity-10" style={{ fontFamily: 'monospace' }}>
              MC
            </div>
            <h2 className="text-2xl font-black tracking-widest text-txt-primary">
              MINECRAFT
            </h2>
          </div>

          {/* Version selector */}
          <div className="flex flex-col items-center gap-3">
            <div className="flex items-center gap-2">
              <label className="flex items-center gap-1.5 text-xs text-txt-secondary cursor-pointer">
                <input
                  type="checkbox"
                  checked={showReleaseOnly}
                  onChange={(e) => setShowReleaseOnly(e.target.checked)}
                  className="accent-accent"
                />
                Releases uniquement
              </label>
            </div>
            <select
              value={selectedVersion}
              onChange={(e) => setSelectedVersion(e.target.value)}
              className="w-48 rounded-xl border border-border bg-bg-card px-4 py-2 text-sm text-txt-primary outline-none transition-theme focus:border-accent"
            >
              {filteredVersions.map((v) => (
                <option key={v.id} value={v.id}>
                  {v.id}{v.version_type === 'snapshot' ? ' (snapshot)' : ''}
                </option>
              ))}
            </select>
          </div>

          {/* RAM slider */}
          <div className="flex flex-col items-center gap-1">
            <label className="text-xs text-txt-secondary">RAM : {ram} Mo</label>
            <input
              type="range"
              min={1024}
              max={16384}
              step={512}
              value={ram}
              onChange={(e) => setRam(Number(e.target.value))}
              className="w-48 accent-accent"
            />
          </div>

          {/* Play button */}
          <button
            onClick={handleLaunch}
            disabled={gameRunning || !selectedVersion}
            className={`glow relative w-48 overflow-hidden rounded-2xl py-4 text-lg font-black tracking-widest text-white transition-all active:scale-95
              ${gameRunning
                ? 'cursor-not-allowed bg-bg-card text-txt-secondary'
                : 'bg-accent hover:bg-accent-hover'
              }`}
          >
            {gameRunning ? 'EN JEU...' : 'JOUER'}
          </button>

          {launchMsg && (
            <p className="text-sm text-red-400">{launchMsg}</p>
          )}

          {/* Progress bar */}
          {progress && progress.downloading && (
            <div className="w-80 flex flex-col gap-1">
              <div className="flex justify-between text-xs text-txt-secondary">
                <span>{progress.message}</span>
                <span>{Math.round(progress.percent)}%</span>
              </div>
              <div className="h-1.5 w-full overflow-hidden rounded-full bg-bg-card">
                <div
                  className="h-full rounded-full bg-accent transition-all duration-300"
                  style={{ width: `${progress.percent}%` }}
                />
              </div>
            </div>
          )}
        </div>

        {/* Bottom bar */}
        <div className="flex items-center justify-between border-t border-border bg-bg-secondary px-6 py-2">
          <span className="text-xs text-txt-secondary">
            Minecraft {selectedVersion}
          </span>
          <div className="flex items-center gap-4">
            <span className="text-xs font-medium text-txt-primary">{username}</span>
            <button
              onClick={handleLogout}
              className="text-xs text-txt-secondary transition hover:text-red-400"
            >
              Déconnexion
            </button>
          </div>
        </div>
      </main>
    </div>
  )
}

function NavIcon({
  children,
  title,
  active,
}: {
  children: React.ReactNode
  title: string
  active?: boolean
}) {
  return (
    <button
      title={title}
      className={`flex h-10 w-10 items-center justify-center rounded-xl transition-theme
        ${active
          ? 'bg-accent text-white'
          : 'text-txt-secondary hover:bg-bg-card hover:text-txt-primary'
        }`}
    >
      {children}
    </button>
  )
}
