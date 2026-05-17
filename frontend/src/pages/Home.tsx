import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '@/api/client'
import { useStore } from '@/stores/useStore'
import type { ProgressResponse } from '@/types'

const STARS = Array.from({ length: 55 }, (_, i) => ({
  x: (i * 37 + ((i * 7 + 13) % 100) * 1.7) % 100,
  y: (i * 23 + ((i * 7 + 13) % 100) * 2.3) % 62,
  r: i % 4 === 0 ? 2 : 1,
  o: 0.15 + (i % 5) * 0.08,
}))

export default function Home() {
  const navigate = useNavigate()
  const {
    username, uuid,
    clearUser,
    versions, setVersions,
    selectedVersion, setSelectedVersion,
    ram,
    gameRunning, setGameRunning,
  } = useStore()

  const [progress, setProgress] = useState<ProgressResponse | null>(null)
  const [launchMsg, setLaunchMsg] = useState('')
  const progressRef = useRef<ReturnType<typeof setInterval> | null>(null)

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

  const filteredVersions = versions.filter((v) => v.version_type === 'release')

  return (
    <div className="flex h-full flex-col overflow-hidden" style={{ background: '#09090D' }}>

      {/* ── Main area: banner + right panel ── */}
      <div className="flex flex-1 gap-4 overflow-hidden p-4">

        {/* LEFT: Cinematic Minecraft banner */}
        <div
          className="relative flex-1 overflow-hidden rounded-[20px]"
          style={{
            border: '1px solid rgba(200,200,220,0.08)',
            boxShadow: '0 8px 40px rgba(0,0,0,0.7)',
          }}
        >
          {/* Night sky gradient */}
          <div
            className="absolute inset-0"
            style={{
              background: 'linear-gradient(180deg, #020208 0%, #06041a 18%, #0e0932 40%, #1c1250 58%, #130d35 76%, #070512 100%)',
            }}
          />
          {/* Atmospheric purple glow */}
          <div className="absolute inset-0" style={{
            background: 'radial-gradient(ellipse at 38% 55%, rgba(75,63,207,0.09) 0%, transparent 55%)',
          }} />
          {/* Green mob glow */}
          <div className="absolute inset-0" style={{
            background: 'radial-gradient(ellipse at 68% 58%, rgba(80,210,80,0.06) 0%, transparent 38%)',
          }} />

          {/* Stars */}
          {STARS.map((s, i) => (
            <div
              key={i}
              className="absolute rounded-full"
              style={{
                left: `${s.x}%`,
                top: `${s.y}%`,
                width: s.r,
                height: s.r,
                background: `rgba(255,255,255,${s.o})`,
              }}
            />
          ))}

          {/* Blocky terrain silhouette */}
          <div className="absolute bottom-0 left-0 right-0" style={{ height: '38%' }}>
            <svg viewBox="0 0 800 220" preserveAspectRatio="none" className="absolute inset-0 h-full w-full">
              <path
                d="M0 220 L0 110 L16 110 L16 90 L32 90 L32 110 L48 110 L48 130 L64 130 L64 100 L80 100 L80 78 L96 78 L96 100 L112 100 L112 120 L128 120 L128 95 L144 95 L144 78 L160 78 L160 95 L176 95 L176 115 L192 115 L192 135 L208 135 L208 115 L224 115 L224 98 L240 98 L240 78 L256 78 L256 95 L272 95 L272 115 L288 115 L288 100 L304 100 L304 82 L320 82 L320 100 L336 100 L336 120 L352 120 L352 100 L368 100 L368 82 L384 82 L384 100 L400 100 L400 118 L416 118 L416 135 L432 135 L432 115 L448 115 L448 95 L464 95 L464 78 L480 78 L480 92 L496 92 L496 110 L512 110 L512 128 L528 128 L528 108 L544 108 L544 88 L560 88 L560 108 L576 108 L576 125 L592 125 L592 140 L608 140 L608 120 L624 120 L624 100 L640 100 L640 80 L656 80 L656 98 L672 98 L672 115 L688 115 L688 100 L704 100 L704 82 L720 82 L720 100 L736 100 L736 118 L752 118 L752 105 L768 105 L768 120 L784 120 L784 140 L800 140 L800 220 Z"
                fill="rgba(4,3,12,0.88)"
              />
            </svg>
          </div>

          {/* Bottom fade */}
          <div className="absolute bottom-0 left-0 right-0 h-24" style={{
            background: 'linear-gradient(to top, rgba(9,9,13,0.95), transparent)',
          }} />

          {/* Play capsule button — top left */}
          <button
            onClick={username ? handleLaunch : () => navigate('/login')}
            disabled={gameRunning || !selectedVersion}
            className="absolute left-4 top-4 flex items-center gap-2 transition-all duration-200"
            style={{
              height: 30,
              paddingLeft: 10,
              paddingRight: 14,
              background: 'rgba(18,15,38,0.78)',
              border: '1px solid rgba(255,255,255,0.22)',
              borderRadius: 20,
              backdropFilter: 'blur(10px)',
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.background = 'rgba(75,63,207,0.32)'
              e.currentTarget.style.borderColor = 'rgba(255,255,255,0.45)'
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.background = 'rgba(18,15,38,0.78)'
              e.currentTarget.style.borderColor = 'rgba(255,255,255,0.22)'
            }}
          >
            {gameRunning ? (
              <span
                className="h-3 w-3 animate-spin-slow rounded-full border-2"
                style={{ borderColor: 'rgba(255,255,255,0.2)', borderTopColor: 'white' }}
              />
            ) : (
              <svg viewBox="0 0 10 10" fill="white" width={9} height={9}>
                <polygon points="1,1 9,5 1,9" />
              </svg>
            )}
            <span className="text-xs font-medium text-white">
              {gameRunning ? 'En jeu...' : 'Play'}
            </span>
          </button>

          {/* Version badge */}
          {selectedVersion && (
            <div className="absolute bottom-4 right-4">
              <span className="text-[11px] font-medium" style={{ color: 'rgba(255,255,255,0.18)' }}>
                {selectedVersion}
              </span>
            </div>
          )}
        </div>

        {/* RIGHT: Launcher panel */}
        <div className="flex w-[28%] flex-shrink-0 flex-col items-center justify-between overflow-hidden px-1 py-5">

          {/* Title */}
          <h1
            className="text-center font-black text-white leading-none"
            style={{
              fontSize: 44,
              textShadow: '0 0 40px rgba(75,63,207,0.55)',
              letterSpacing: '-0.01em',
            }}
          >
            YuyuFrame
          </h1>

          {/* Avatar / Connect */}
          <div className="flex flex-col items-center gap-2">
            {username ? (
              <button
                onClick={() => navigate('/login')}
                className="flex flex-col items-center gap-2 group"
                title="Gérer les comptes"
              >
                <div className="relative">
                  {uuid && (
                    <img
                      src={`https://mc-heads.net/avatar/${uuid}/100`}
                      alt={username}
                      className="rounded-xl transition-all duration-200 group-hover:brightness-75"
                      style={{
                        width: 100, height: 100,
                        imageRendering: 'pixelated',
                        boxShadow: '0 4px 24px rgba(0,0,0,0.6)',
                      }}
                      onError={(e) => {
                        e.currentTarget.style.display = 'none'
                        const fb = e.currentTarget.nextElementSibling as HTMLElement | null
                        if (fb) fb.style.display = 'flex'
                      }}
                    />
                  )}
                  <div
                    className="items-center justify-center rounded-xl font-black text-white text-4xl transition-all duration-200 group-hover:brightness-75"
                    style={{ width: 100, height: 100, background: 'rgba(75,63,207,0.55)', fontFamily: 'monospace', display: uuid ? 'none' : 'flex' }}
                  >
                    {username[0].toUpperCase()}
                  </div>
                  <div className="absolute inset-0 flex items-center justify-center rounded-xl opacity-0 group-hover:opacity-100 transition-opacity duration-200">
                    <svg viewBox="0 0 24 24" fill="white" style={{ width: 24, height: 24, opacity: 0.9 }}>
                      <path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z" />
                    </svg>
                  </div>
                </div>
                <span style={{ fontSize: 11, color: 'rgba(255,255,255,0.4)', fontWeight: 500 }}>
                  {username}
                </span>
              </button>
            ) : (
              <button
                onClick={() => navigate('/login')}
                className="flex flex-col items-center justify-center gap-2 rounded-xl transition-all duration-200"
                style={{
                  width: 100, height: 100,
                  border: '2px dashed rgba(255,255,255,0.1)',
                  color: 'rgba(255,255,255,0.25)',
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.borderColor = 'rgba(75,63,207,0.5)'
                  e.currentTarget.style.color = 'rgba(120,110,230,0.7)'
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.borderColor = 'rgba(255,255,255,0.1)'
                  e.currentTarget.style.color = 'rgba(255,255,255,0.25)'
                }}
              >
                <svg viewBox="0 0 24 24" fill="currentColor" className="h-8 w-8">
                  <path d="M11 7L9.6 8.4l2.6 2.6H2v2h10.2l-2.6 2.6L11 17l5-5-5-5zm9 12h-8v2h8c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2h-8v2h8v14z" />
                </svg>
                <span style={{ fontSize: 10, letterSpacing: '0.1em', fontWeight: 600 }}>SE CONNECTER</span>
              </button>
            )}
          </div>

          {/* Divider */}
          <div className="w-full h-px" style={{ background: 'rgba(255,255,255,0.06)' }} />

          {/* Version selector */}
          <div className="w-full">
            <div className="mb-1.5">
              <label style={{ fontSize: 10, color: 'rgba(255,255,255,0.4)', letterSpacing: '0.1em', textTransform: 'uppercase', fontWeight: 600 }}>
                Version
              </label>
            </div>
            <div className="relative w-full">
              <select
                value={selectedVersion}
                onChange={(e) => setSelectedVersion(e.target.value)}
                className="w-full appearance-none rounded-xl px-3 pr-8 text-sm font-medium text-white outline-none"
                style={{
                  height: 45,
                  background: 'rgba(0,0,0,0.45)',
                  border: '1px solid rgba(255,255,255,0.1)',
                }}
              >
                {filteredVersions.length === 0 && <option value="">Chargement...</option>}
                {filteredVersions.map((v) => (
                  <option key={v.id} value={v.id} style={{ background: '#111118', color: 'white' }}>
                    {v.id}{v.version_type === 'snapshot' ? ' (snapshot)' : ''}
                  </option>
                ))}
              </select>
              <div className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2">
                <svg viewBox="0 0 10 6" fill="white" width={10} height={6} style={{ opacity: 0.45 }}>
                  <path d="M0 0l5 6 5-6z" />
                </svg>
              </div>
            </div>
          </div>

          {/* Launch button */}
          <button
            onClick={username ? handleLaunch : () => navigate('/login')}
            disabled={gameRunning || !selectedVersion}
            className="w-full font-bold text-white transition-all duration-200 active:scale-95"
            style={{
              height: 60,
              borderRadius: 16,
              fontSize: 14,
              letterSpacing: '0.04em',
              background: gameRunning || !selectedVersion ? 'rgba(40,38,65,0.7)' : '#4B3FCF',
              boxShadow: !gameRunning && selectedVersion ? '0 4px 28px rgba(75,63,207,0.42)' : 'none',
              cursor: gameRunning || !selectedVersion ? 'not-allowed' : 'pointer',
            }}
            onMouseEnter={(e) => {
              if (!gameRunning && selectedVersion) {
                e.currentTarget.style.background = '#6155e8'
                e.currentTarget.style.boxShadow = '0 6px 32px rgba(75,63,207,0.62)'
              }
            }}
            onMouseLeave={(e) => {
              if (!gameRunning && selectedVersion) {
                e.currentTarget.style.background = '#4B3FCF'
                e.currentTarget.style.boxShadow = '0 4px 28px rgba(75,63,207,0.42)'
              }
            }}
          >
            {gameRunning ? (
              <span className="flex items-center justify-center gap-2">
                <span
                  className="h-4 w-4 animate-spin-slow rounded-full border-2"
                  style={{ borderColor: 'rgba(255,255,255,0.2)', borderTopColor: 'white' }}
                />
                EN JEU...
              </span>
            ) : username ? (
              `lancer ${selectedVersion || '...'}`
            ) : (
              'SE CONNECTER'
            )}
          </button>

          {launchMsg && (
            <p
              className="w-full rounded-lg px-3 py-2 text-center text-xs text-red-300"
              style={{ background: 'rgba(200,50,50,0.12)' }}
            >
              {launchMsg}
            </p>
          )}

          {/* Progress */}
          {progress && progress.downloading && (
            <div className="w-full flex flex-col gap-1.5">
              <div className="flex justify-between" style={{ fontSize: 10, color: 'rgba(255,255,255,0.4)' }}>
                <span className="truncate">{progress.message}</span>
                <span className="ml-2 flex-shrink-0">{Math.round(progress.percent)}%</span>
              </div>
              <div className="h-1 w-full overflow-hidden rounded-full" style={{ background: 'rgba(0,0,0,0.4)' }}>
                <div
                  className="h-full rounded-full transition-all duration-300"
                  style={{ width: `${progress.percent}%`, background: '#4B3FCF' }}
                />
              </div>
            </div>
          )}
        </div>
      </div>

      {/* ── Footer ── */}
      <div
        className="flex overflow-hidden"
        style={{ flexShrink: 0, flexBasis: '30%', background: '#09090D', borderTop: '1px solid rgba(255,255,255,0.05)' }}
      >
        {/* Left: legal links */}
        <div
          className="flex flex-col justify-center gap-3 px-7 py-5"
          style={{
            width: '14%',
            background: 'rgba(255,255,255,0.015)',
            borderRight: '1px solid rgba(255,255,255,0.05)',
          }}
        >
          <span style={{ fontSize: 10, color: 'rgba(255,255,255,0.18)', marginBottom: 2 }}>© 2025</span>
          {['ABOUT', 'Politique', 'EULA', 'README', 'Licence'].map((item) => (
            <span
              key={item}
              className="cursor-pointer transition-colors duration-150"
              style={{ fontSize: 11, color: 'rgba(255,255,255,0.2)' }}
              onMouseEnter={(e) => { (e.currentTarget as HTMLElement).style.color = 'rgba(255,255,255,0.55)' }}
              onMouseLeave={(e) => { (e.currentTarget as HTMLElement).style.color = 'rgba(255,255,255,0.2)' }}
            >
              {item}
            </span>
          ))}
        </div>

        {/* Center: Quêtes */}
        <div className="flex flex-1 flex-col">
          <div className="flex items-center gap-4 px-10 pt-6">
            <div className="flex-1 h-px" style={{ background: 'rgba(255,255,255,0.07)' }} />
            <span className="font-bold text-white tracking-widest uppercase" style={{ fontSize: 13 }}>Quêtes</span>
            <div className="flex-1 h-px" style={{ background: 'rgba(255,255,255,0.07)' }} />
          </div>
          <div className="flex flex-1 items-center justify-center">
            <span style={{ fontSize: 11, color: 'rgba(255,255,255,0.12)' }}>Aucune quête active</span>
          </div>
        </div>

        {/* Right: Navigation */}
        <div
          className="flex flex-col"
          style={{ width: '28%', borderLeft: '1px solid rgba(255,255,255,0.05)' }}
        >
          {/* Titre */}
          <div className="flex items-center gap-4 px-6 pt-6">
            <div className="flex-1 h-px" style={{ background: 'rgba(255,255,255,0.07)' }} />
            <span className="font-bold text-white tracking-widest uppercase" style={{ fontSize: 13 }}>Navigation</span>
            <div className="flex-1 h-px" style={{ background: 'rgba(255,255,255,0.07)' }} />
          </div>

          {/* Zone centrale libre */}
          <div className="flex-1" />

          {/* Icônes utilitaires */}
          <div className="flex items-center justify-center gap-3 pb-2">
            <FIcon title="Infos">
              <svg viewBox="0 0 24 24" fill="currentColor" className="h-5 w-5">
                <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z" />
              </svg>
            </FIcon>
            <FIcon title="Discord">
              <svg viewBox="0 0 24 24" fill="currentColor" className="h-5 w-5">
                <path d="M20.317 4.37a19.791 19.791 0 00-4.885-1.515.074.074 0 00-.079.037c-.21.375-.444.864-.608 1.25a18.27 18.27 0 00-5.487 0 12.64 12.64 0 00-.617-1.25.077.077 0 00-.079-.037A19.736 19.736 0 003.677 4.37a.07.07 0 00-.032.027C.533 9.046-.32 13.58.099 18.057a.082.082 0 00.031.057 19.9 19.9 0 005.993 3.03.078.078 0 00.084-.028c.462-.63.874-1.295 1.226-1.994a.076.076 0 00-.041-.106 13.107 13.107 0 01-1.872-.892.077.077 0 01-.008-.128 10.2 10.2 0 00.372-.292.074.074 0 01.077-.01c3.928 1.793 8.18 1.793 12.062 0a.074.074 0 01.078.01c.12.098.246.198.373.292a.077.077 0 01-.006.127 12.299 12.299 0 01-1.873.892.077.077 0 00-.041.107c.36.698.772 1.362 1.225 1.993a.076.076 0 00.084.028 19.839 19.839 0 006.002-3.03.077.077 0 00.032-.054c.5-5.177-.838-9.674-3.549-13.66a.061.061 0 00-.031-.03z" />
              </svg>
            </FIcon>
            <FIcon title="Paramètres" onClick={() => navigate('/settings')}>
              <svg viewBox="0 0 24 24" fill="currentColor" className="h-5 w-5">
                <path d="M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z" />
              </svg>
            </FIcon>
            <FIcon title="Plugins">
              <svg viewBox="0 0 24 24" fill="currentColor" className="h-5 w-5">
                <path d="M21 16.5c0 .38-.21.71-.53.88l-7.9 4.44c-.16.12-.36.18-.57.18s-.41-.06-.57-.18l-7.9-4.44A1 1 0 013 16.5v-9c0-.38.21-.71.53-.88l7.9-4.44c.16-.12.36-.18.57-.18s.41.06.57.18l7.9 4.44c.32.17.53.5.53.88v9zM12 4.15L6.04 7.5 12 10.85l5.96-3.35L12 4.15zM5 15.91l6 3.38v-6.71L5 9.21v6.7zm14 0v-6.7l-6 3.37v6.71l6-3.38z" />
              </svg>
            </FIcon>
          </div>

          {/* Séparateur */}
          <div className="mx-6 h-px" style={{ background: 'rgba(255,255,255,0.05)' }} />

          {/* Icônes de navigation */}
          <div className="flex items-center justify-center gap-2 px-4 pb-5 pt-2">
            <NIcon title="Login" active={!!username} label="Login" onClick={() => navigate('/login')}>
              <svg viewBox="0 0 24 24" fill="currentColor" className="h-5 w-5">
                <path d="M11 7L9.6 8.4l2.6 2.6H2v2h10.2l-2.6 2.6L11 17l5-5-5-5zm9 12h-8v2h8c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2h-8v2h8v14z" />
              </svg>
            </NIcon>
            <NIcon title="Serveurs (bientôt)" disabled>
              <svg viewBox="0 0 24 24" fill="currentColor" className="h-5 w-5">
                <path d="M20 3H4v10c0 1.1.9 2 2 2h2v2H6v2h12v-2h-2v-2h2c1.1 0 2-.9 2-2V3zm-2 10H6V5h12v8z" />
              </svg>
            </NIcon>
            <NIcon title="Mods (bientôt)" disabled>
              <svg viewBox="0 0 24 24" fill="currentColor" className="h-5 w-5">
                <path d="M20.5 11H19V7c0-1.1-.9-2-2-2h-4V3.5C13 2.12 11.88 1 10.5 1S8 2.12 8 3.5V5H4c-1.1 0-1.99.9-1.99 2v3.8H3.5c1.49 0 2.7 1.21 2.7 2.7s-1.21 2.7-2.7 2.7H2V20c0 1.1.9 2 2 2h3.8v-1.5c0-1.49 1.21-2.7 2.7-2.7 1.49 0 2.7 1.21 2.7 2.7V22H17c1.1 0 2-.9 2-2v-4h1.5c1.38 0 2.5-1.12 2.5-2.5S21.88 11 20.5 11z" />
              </svg>
            </NIcon>
            <NIcon title="Boutique (bientôt)" disabled>
              <svg viewBox="0 0 24 24" fill="currentColor" className="h-5 w-5">
                <path d="M7 18c-1.1 0-1.99.9-1.99 2S5.9 22 7 22s2-.9 2-2-.9-2-2-2zM1 2v2h2l3.6 7.59-1.35 2.45c-.16.28-.25.61-.25.96C5 16.1 6.9 18 9 18h12v-2H9.42c-.14 0-.25-.11-.25-.25l.03-.12.9-1.63H19c.75 0 1.41-.41 1.75-1.03l3.58-6.49A1 1 0 0023.45 5H5.21l-.94-2H1zm16 16c-1.1 0-1.99.9-1.99 2s.89 2 1.99 2 2-.9 2-2-.9-2-2-2z" />
              </svg>
            </NIcon>
            <NIcon title="Statistiques (bientôt)" disabled>
              <svg viewBox="0 0 24 24" fill="currentColor" className="h-5 w-5">
                <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM9 17H7v-7h2v7zm4 0h-2V7h2v10zm4 0h-2v-4h2v4z" />
              </svg>
            </NIcon>
          </div>

          {username && (
            <div className="flex justify-center pb-4">
              <button
                onClick={handleLogout}
                className="transition-colors duration-150"
                style={{ fontSize: 10, color: 'rgba(255,255,255,0.2)' }}
                onMouseEnter={(e) => { (e.currentTarget as HTMLElement).style.color = 'rgb(248,113,113)' }}
                onMouseLeave={(e) => { (e.currentTarget as HTMLElement).style.color = 'rgba(255,255,255,0.2)' }}
              >
                Déconnexion
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

function FIcon({ children, title, onClick }: { children: React.ReactNode; title: string; onClick?: () => void }) {
  return (
    <button
      title={title}
      onClick={onClick}
      className="flex h-10 w-10 items-center justify-center rounded-xl transition-all duration-150"
      style={{ color: 'rgba(255,255,255,0.25)' }}
      onMouseEnter={(e) => {
        e.currentTarget.style.color = 'rgba(255,255,255,0.7)'
        e.currentTarget.style.background = 'rgba(255,255,255,0.04)'
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.color = 'rgba(255,255,255,0.25)'
        e.currentTarget.style.background = 'transparent'
      }}
    >
      {children}
    </button>
  )
}

function NIcon({
  children, title, active, label, disabled, onClick,
}: {
  children: React.ReactNode
  title: string
  active?: boolean
  label?: string
  disabled?: boolean
  onClick?: () => void
}) {
  return (
    <div className="flex flex-col items-center gap-0.5">
      <button
        title={title}
        onClick={disabled ? undefined : onClick}
        className="flex h-10 w-10 items-center justify-center rounded-xl transition-all duration-150"
        style={{
          color: disabled ? 'rgba(255,255,255,0.12)' : active ? '#7dd3fc' : 'rgba(255,255,255,0.3)',
          background: active ? 'rgba(125,211,252,0.08)' : 'transparent',
          boxShadow: active ? '0 0 14px rgba(125,211,252,0.18)' : 'none',
          cursor: disabled ? 'not-allowed' : 'pointer',
        }}
        onMouseEnter={(e) => {
          if (!disabled && !active) {
            e.currentTarget.style.color = 'rgba(255,255,255,0.65)'
            e.currentTarget.style.background = 'rgba(255,255,255,0.05)'
          }
        }}
        onMouseLeave={(e) => {
          if (!disabled && !active) {
            e.currentTarget.style.color = 'rgba(255,255,255,0.3)'
            e.currentTarget.style.background = 'transparent'
          }
        }}
      >
        {children}
      </button>
      {active && label && (
        <span style={{ fontSize: 8, color: 'rgba(125,211,252,0.5)' }}>{label}</span>
      )}
    </div>
  )
}
