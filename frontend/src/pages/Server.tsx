import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { listen } from '@tauri-apps/api/event'
import { getCurrentWindow } from '@tauri-apps/api/window'
import { api } from '@/api/client'
import { useStore } from '@/stores/useStore'

export default function Server() {
  const navigate = useNavigate()
  const {
    username,
    instances, setInstances,
    selectedInstanceId, selectedInstance,
    gameRunning, setGameRunning,
    closeOnLaunch,
  } = useStore()

  const [launchMsg, setLaunchMsg] = useState('')
  const instance = selectedInstance()

  useEffect(() => {
    api.instances.list().then(setInstances).catch(() => {})
  }, [])

  useEffect(() => {
    let unlistenState: (() => void) | null = null
    let unlistenError: (() => void) | null = null

    listen<{ running: boolean }>('game_state', (event) => {
      setGameRunning(event.payload.running)
      if (!event.payload.running) getCurrentWindow().show()
    }).then((fn) => { unlistenState = fn })

    listen<string>('launch_error', (event) => {
      setLaunchMsg(event.payload)
      setGameRunning(false)
    }).then((fn) => { unlistenError = fn })

    return () => {
      unlistenState?.()
      unlistenError?.()
    }
  }, [])

  const handleLaunchP2p = async () => {
    if (!selectedInstanceId || gameRunning || !username) return
    setLaunchMsg('')
    try {
      await api.launch.startP2p(selectedInstanceId)
      setGameRunning(true)
      if (closeOnLaunch) getCurrentWindow().hide()
    } catch (e) {
      setLaunchMsg(e instanceof Error ? e.message : 'Erreur de lancement')
    }
  }

  const canLaunch = !!selectedInstanceId && !!username && !gameRunning

  return (
    <div className="flex h-full flex-col items-center justify-center gap-8" style={{ background: '#09090D' }}>

      {/* Header */}
      <div className="flex flex-col items-center gap-2">
        <div className="flex items-center justify-center rounded-2xl" style={{ width: 56, height: 56, background: 'rgba(75,63,207,0.18)', border: '1px solid rgba(75,63,207,0.35)' }}>
          <svg viewBox="0 0 24 24" fill="rgba(120,110,230,0.9)" width={26} height={26}>
            <path d="M4 6h16v2H4zm0 5h16v2H4zm0 5h16v2H4z" />
          </svg>
        </div>
        <h1 className="font-black text-white" style={{ fontSize: 28, letterSpacing: '-0.01em', textShadow: '0 0 32px rgba(75,63,207,0.5)' }}>
          Serveur P2P
        </h1>
        <p style={{ fontSize: 13, color: 'rgba(255,255,255,0.3)', textAlign: 'center', maxWidth: 320 }}>
          Lance Minecraft avec le mode P2P activé.<br />
          {instance ? (
            <span style={{ color: 'rgba(120,110,230,0.7)', fontWeight: 600 }}>{instance.name} — {instance.mc_version}</span>
          ) : (
            <span style={{ color: 'rgba(255,100,100,0.6)' }}>Aucune instance sélectionnée</span>
          )}
        </p>
      </div>

      {/* Launch button */}
      <button
        onClick={username ? handleLaunchP2p : () => navigate('/login')}
        disabled={gameRunning || (!!username && !selectedInstanceId)}
        className="font-bold text-white transition-all duration-200 active:scale-95"
        style={{
          width: 280, height: 64, borderRadius: 18, fontSize: 15, letterSpacing: '0.06em',
          background: canLaunch ? '#4B3FCF' : !username ? 'rgba(75,63,207,0.45)' : 'rgba(40,38,65,0.7)',
          boxShadow: canLaunch ? '0 6px 36px rgba(75,63,207,0.5)' : 'none',
          cursor: (gameRunning || (!!username && !selectedInstanceId)) ? 'not-allowed' : 'pointer',
          border: '1px solid rgba(120,110,230,0.25)',
        }}
        onMouseEnter={(e) => { if (canLaunch) { e.currentTarget.style.background = '#6155e8'; e.currentTarget.style.boxShadow = '0 8px 40px rgba(75,63,207,0.65)' } }}
        onMouseLeave={(e) => { if (canLaunch) { e.currentTarget.style.background = '#4B3FCF'; e.currentTarget.style.boxShadow = '0 6px 36px rgba(75,63,207,0.5)' } }}
      >
        {gameRunning ? (
          <span className="flex items-center justify-center gap-2">
            <span className="h-4 w-4 animate-spin-slow rounded-full border-2" style={{ borderColor: 'rgba(255,255,255,0.2)', borderTopColor: 'white' }} />
            EN JEU...
          </span>
        ) : !username ? 'SE CONNECTER'
          : !selectedInstanceId ? 'AUCUNE INSTANCE'
          : 'LANCER EN MODE P2P'}
      </button>

      {launchMsg && (
        <p className="rounded-lg px-4 py-2 text-center text-xs text-red-300" style={{ background: 'rgba(200,50,50,0.12)', maxWidth: 320 }}>
          {launchMsg}
        </p>
      )}

      {/* Back */}
      <button
        onClick={() => navigate('/home')}
        className="transition-colors duration-150"
        style={{ fontSize: 12, color: 'rgba(255,255,255,0.22)', fontWeight: 500 }}
        onMouseEnter={(e) => { (e.currentTarget as HTMLElement).style.color = 'rgba(255,255,255,0.6)' }}
        onMouseLeave={(e) => { (e.currentTarget as HTMLElement).style.color = 'rgba(255,255,255,0.22)' }}
      >
        ← Retour
      </button>
    </div>
  )
}
