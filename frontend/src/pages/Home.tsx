import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '@/api/client'
import { useStore } from '@/stores/useStore'
import { Sidebar } from '@/components/Sidebar'
import { Background, useChillPalette } from '@/components/Background'
import type { ProgressResponse } from '@/types'

export default function Home() {
  const navigate = useNavigate()
  const {
    username, uuid,
    clearUser,
    versions, setVersions,
    selectedVersion, setSelectedVersion,
    ram, setRam,
    gameRunning, setGameRunning,
    theme,
  } = useStore()

  const [progress, setProgress] = useState<ProgressResponse | null>(null)
  const [launchMsg, setLaunchMsg] = useState('')
  const [showReleaseOnly, setShowReleaseOnly] = useState(true)
  const progressRef = useRef<ReturnType<typeof setInterval> | null>(null)
  const palette = useChillPalette()

  useEffect(() => {
    api.versions.list().then((v) => {
      setVersions(v)
      if (!selectedVersion && v.length > 0) {
        const latest = v.find((ver) => ver.version_type === 'release')
        setSelectedVersion(latest?.id ?? v[0].id)
      }
    }).catch(() => {})
  }, [])

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
        } catch { /* ignore */ }
      }, 1000)
    } else {
      if (progressRef.current) { clearInterval(progressRef.current); progressRef.current = null }
    }
    return () => { if (progressRef.current) clearInterval(progressRef.current) }
  }, [gameRunning])

  const handleLogout = async () => {
    await api.auth.logout()
    clearUser()
    navigate('/login', { replace: true })
  }

  const handleLaunch = async () => {
    if (!selectedVersion || gameRunning || !username) return
    setLaunchMsg('')
    try {
      const resp = await api.launch.start(selectedVersion, ram)
      if (resp.success) setGameRunning(true)
      else setLaunchMsg(resp.message)
    } catch (e) {
      setLaunchMsg(e instanceof Error ? e.message : 'Erreur de lancement')
    }
  }

  const filteredVersions = showReleaseOnly
    ? versions.filter((v) => v.version_type === 'release')
    : versions

  const isGamer = theme === 'gamer'

  // Controls panel background matches the theme
  const panelBg = isGamer ? 'rgb(40,40,40)' : palette.s2
  const panelBorder = isGamer ? 'rgb(60,60,60)' : 'rgba(0,0,0,0.15)'

  return (
    <div className="flex h-full overflow-hidden">
      <Sidebar />

      <div className="flex flex-1 flex-col overflow-hidden">
        {/* ── Main area: art left + controls right ── */}
        <div className="flex flex-1 overflow-hidden">
          {/* LEFT: Art / Background area */}
          <div className="relative flex-1 overflow-hidden">
            <Background />

            {/* "YuyuFrame" watermark on the art area */}
            <div className="absolute bottom-6 left-6 select-none">
              <span
                className="font-black text-5xl tracking-[0.3em] opacity-10"
                style={{
                  color: 'white',
                  fontFamily: 'monospace',
                }}
              >
                MC
              </span>
            </div>
          </div>

          {/* RIGHT: Controls panel — 288px, matching original ~300px */}
          <div
            className="relative flex w-72 flex-shrink-0 flex-col overflow-y-auto p-5"
            style={{ background: panelBg, borderLeft: `1px solid ${panelBorder}` }}
          >
            {/* Title + Avatar row */}
            <div className="flex items-start justify-between mb-6">
              <div>
                <h1
                  className="font-black text-2xl tracking-widest text-white"
                  style={{ fontFamily: 'monospace', textShadow: isGamer ? '0 0 16px rgb(68 62 185 / 0.7)' : 'none' }}
                >
                  YUYUFRAME
                </h1>
                <p className="text-[10px] tracking-widest text-white/50 uppercase mt-0.5">
                  Minecraft Launcher
                </p>
              </div>

              {username && (
                <div className="flex flex-col items-center gap-1">
                  {uuid ? (
                    <img
                      src={`https://crafatar.com/avatars/${uuid}?size=100&overlay`}
                      alt={username}
                      className="h-[72px] w-[72px] rounded-lg"
                      style={{ imageRendering: 'pixelated' }}
                      onError={(e) => {
                        const el = e.currentTarget
                        el.style.display = 'none'
                        const fallback = el.nextElementSibling as HTMLElement | null
                        if (fallback) fallback.style.display = 'flex'
                      }}
                    />
                  ) : null}
                  <div
                    className="h-[72px] w-[72px] items-center justify-center rounded-lg bg-accent text-3xl font-black text-white"
                    style={{ display: uuid ? 'none' : 'flex', fontFamily: 'monospace' }}
                  >
                    {username[0].toUpperCase()}
                  </div>
                  <span className="text-[10px] font-medium text-white/60 mt-0.5">{username}</span>
                </div>
              )}

              {!username && (
                <button
                  onClick={() => navigate('/login')}
                  className="flex h-[72px] w-[72px] flex-col items-center justify-center gap-1 rounded-lg border-2 border-dashed border-white/20 text-white/40 transition hover:border-accent hover:text-accent"
                >
                  <svg viewBox="0 0 24 24" fill="currentColor" className="h-6 w-6">
                    <path d="M11 7L9.6 8.4l2.6 2.6H2v2h10.2l-2.6 2.6L11 17l5-5-5-5zm9 12h-8v2h8c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2h-8v2h8v14z" />
                  </svg>
                  <span className="text-[9px] tracking-wider">CONNEXION</span>
                </button>
              )}
            </div>

            {/* Divider */}
            <div className="mb-4 h-px w-full" style={{ background: 'rgba(255,255,255,0.1)' }} />

            {/* Version selector */}
            <div className="mb-3">
              <div className="mb-1.5 flex items-center justify-between">
                <label className="text-[11px] font-semibold tracking-wider text-white/60 uppercase">
                  Version
                </label>
                <label className="flex cursor-pointer items-center gap-1 text-[10px] text-white/40 select-none">
                  <input
                    type="checkbox"
                    checked={showReleaseOnly}
                    onChange={(e) => setShowReleaseOnly(e.target.checked)}
                    className="h-2.5 w-2.5 accent-accent"
                  />
                  Releases
                </label>
              </div>
              <select
                value={selectedVersion}
                onChange={(e) => setSelectedVersion(e.target.value)}
                className="w-full rounded-xl px-3 py-2.5 text-sm font-medium text-white outline-none"
                style={{
                  background: 'rgba(0,0,0,0.35)',
                  border: '1px solid rgba(255,255,255,0.15)',
                }}
              >
                {filteredVersions.length === 0 && <option value="">Chargement...</option>}
                {filteredVersions.map((v) => (
                  <option
                    key={v.id}
                    value={v.id}
                    style={{ background: '#1a1a2e', color: 'white' }}
                  >
                    {v.id}{v.version_type === 'snapshot' ? ' (snapshot)' : ''}
                  </option>
                ))}
              </select>
            </div>

            {/* RAM slider */}
            <div className="mb-5">
              <div className="mb-1.5 flex items-center justify-between">
                <label className="text-[11px] font-semibold tracking-wider text-white/60 uppercase">
                  RAM
                </label>
                <span className="text-xs font-bold text-white">
                  {(ram / 1024).toFixed(1)} Go
                </span>
              </div>
              <input
                type="range"
                min={1024}
                max={16384}
                step={512}
                value={ram}
                onChange={(e) => setRam(Number(e.target.value))}
                className="w-full"
                style={{ accentColor: 'rgb(68,62,185)' }}
              />
              <div className="mt-1 flex justify-between text-[9px] text-white/30">
                <span>1 Go</span>
                <span>8 Go</span>
                <span>16 Go</span>
              </div>
            </div>

            {/* Play button */}
            <button
              onClick={username ? handleLaunch : () => navigate('/login')}
              disabled={gameRunning || !selectedVersion}
              className="mb-3 w-full rounded-xl py-4 text-base font-black tracking-[0.2em] text-white transition-all duration-150 active:scale-95"
              style={{
                background: gameRunning || !selectedVersion
                  ? 'rgba(60,60,60,0.8)'
                  : 'rgb(68,62,185)',
                boxShadow: !gameRunning && selectedVersion
                  ? '0 4px 20px rgb(68 62 185 / 0.5)'
                  : 'none',
              }}
              onMouseEnter={(e) => {
                if (!gameRunning && selectedVersion)
                  e.currentTarget.style.background = 'rgb(88,82,220)'
              }}
              onMouseLeave={(e) => {
                if (!gameRunning && selectedVersion)
                  e.currentTarget.style.background = 'rgb(68,62,185)'
              }}
            >
              {gameRunning ? (
                <span className="flex items-center justify-center gap-2">
                  <span
                    className="h-4 w-4 rounded-full border-2 animate-spin-slow"
                    style={{ borderColor: 'rgba(255,255,255,0.2)', borderTopColor: 'white' }}
                  />
                  EN JEU...
                </span>
              ) : username ? (
                'JOUER'
              ) : (
                'SE CONNECTER'
              )}
            </button>

            {launchMsg && (
              <p className="mb-2 rounded-lg px-3 py-2 text-center text-xs text-red-300"
                 style={{ background: 'rgba(200,50,50,0.2)' }}>
                {launchMsg}
              </p>
            )}

            {/* Progress bar */}
            {progress && progress.downloading && (
              <div className="mt-1 flex flex-col gap-1.5">
                <div className="flex justify-between text-[10px] text-white/50">
                  <span className="truncate">{progress.message}</span>
                  <span className="ml-2 flex-shrink-0">{Math.round(progress.percent)}%</span>
                </div>
                <div className="h-1 w-full overflow-hidden rounded-full" style={{ background: 'rgba(0,0,0,0.3)' }}>
                  <div
                    className="h-full rounded-full transition-all duration-300"
                    style={{ width: `${progress.percent}%`, background: 'rgb(68,62,185)' }}
                  />
                </div>
              </div>
            )}
          </div>
        </div>

        {/* ── Bottom navigation bar ── */}
        <div
          className="flex flex-shrink-0 items-center px-4"
          style={{
            height: 52,
            background: isGamer ? 'rgb(10,10,12)' : 'rgba(0,0,0,0.45)',
            borderTop: `1px solid ${isGamer ? 'rgb(50,45,90)' : 'rgba(0,0,0,0.3)'}`,
          }}
        >
          {/* Status dot + version */}
          <div className="flex items-center gap-2 text-xs text-white/40">
            <span
              className={`inline-flex h-1.5 w-1.5 rounded-full ${gameRunning ? 'bg-green-400' : 'bg-white/20'}`}
            />
            {selectedVersion || 'Aucune version'}
          </div>

          {/* Nav icons */}
          <div className="mx-auto flex items-center gap-1">
            <NavBtn title="Paramètres" onClick={() => navigate('/settings')}>
              <svg viewBox="0 0 24 24" fill="currentColor" className="h-5 w-5">
                <path d="M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z" />
              </svg>
            </NavBtn>
            <NavBtn title="Serveurs (bientôt)" disabled>
              <svg viewBox="0 0 24 24" fill="currentColor" className="h-5 w-5">
                <path d="M20 3H4v10c0 1.1.9 2 2 2h2v2H6v2h12v-2h-2v-2h2c1.1 0 2-.9 2-2V3zm-2 10H6V5h12v8z" />
              </svg>
            </NavBtn>
            <NavBtn title="Mods (bientôt)" disabled>
              <svg viewBox="0 0 24 24" fill="currentColor" className="h-5 w-5">
                <path d="M20.5 11H19V7c0-1.1-.9-2-2-2h-4V3.5C13 2.12 11.88 1 10.5 1S8 2.12 8 3.5V5H4c-1.1 0-1.99.9-1.99 2v3.8H3.5c1.49 0 2.7 1.21 2.7 2.7s-1.21 2.7-2.7 2.7H2V20c0 1.1.9 2 2 2h3.8v-1.5c0-1.49 1.21-2.7 2.7-2.7 1.49 0 2.7 1.21 2.7 2.7V22H17c1.1 0 2-.9 2-2v-4h1.5c1.38 0 2.5-1.12 2.5-2.5S21.88 11 20.5 11z" />
              </svg>
            </NavBtn>
            <NavBtn title="Discord (bientôt)" disabled>
              <svg viewBox="0 0 24 24" fill="currentColor" className="h-5 w-5">
                <path d="M20.317 4.37a19.791 19.791 0 00-4.885-1.515.074.074 0 00-.079.037c-.21.375-.444.864-.608 1.25a18.27 18.27 0 00-5.487 0 12.64 12.64 0 00-.617-1.25.077.077 0 00-.079-.037A19.736 19.736 0 003.677 4.37a.07.07 0 00-.032.027C.533 9.046-.32 13.58.099 18.057a.082.082 0 00.031.057 19.9 19.9 0 005.993 3.03.078.078 0 00.084-.028c.462-.63.874-1.295 1.226-1.994a.076.076 0 00-.041-.106 13.107 13.107 0 01-1.872-.892.077.077 0 01-.008-.128 10.2 10.2 0 00.372-.292.074.074 0 01.077-.01c3.928 1.793 8.18 1.793 12.062 0a.074.074 0 01.078.01c.12.098.246.198.373.292a.077.077 0 01-.006.127 12.299 12.299 0 01-1.873.892.077.077 0 00-.041.107c.36.698.772 1.362 1.225 1.993a.076.076 0 00.084.028 19.839 19.839 0 006.002-3.03.077.077 0 00.032-.054c.5-5.177-.838-9.674-3.549-13.66a.061.061 0 00-.031-.03z" />
              </svg>
            </NavBtn>
          </div>

          {/* User info */}
          <div className="flex items-center gap-3">
            {username ? (
              <>
                <span className="text-xs font-medium text-white/60">{username}</span>
                <button
                  onClick={handleLogout}
                  className="text-xs text-white/30 transition hover:text-red-400"
                >
                  Déconnexion
                </button>
              </>
            ) : (
              <button
                onClick={() => navigate('/login')}
                className="text-xs text-white/40 transition hover:text-white"
              >
                Se connecter →
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

function NavBtn({
  children,
  title,
  onClick,
  disabled,
}: {
  children: React.ReactNode
  title: string
  onClick?: () => void
  disabled?: boolean
}) {
  return (
    <button
      title={title}
      onClick={onClick}
      disabled={disabled}
      className={[
        'flex h-9 w-9 items-center justify-center rounded-lg transition-all',
        disabled
          ? 'cursor-not-allowed text-white/15'
          : 'text-white/40 hover:bg-white/10 hover:text-white',
      ].join(' ')}
    >
      {children}
    </button>
  )
}
