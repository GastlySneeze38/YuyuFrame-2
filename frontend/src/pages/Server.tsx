import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { listen } from '@tauri-apps/api/event'
import { getCurrentWindow } from '@tauri-apps/api/window'
import { api } from '@/api/client'
import { useStore } from '@/stores/useStore'

type Mode = 'idle' | 'hosting' | 'joining'
type SessionState = 'none' | 'starting' | 'ready' | 'connecting' | 'connected'

export default function Server() {
  const navigate = useNavigate()
  const {
    username,
    selectedInstanceId, selectedInstance,
    isInstanceRunning, setInstanceRunning,
    closeOnLaunch,
  } = useStore()

  const gameRunning = !!selectedInstanceId && isInstanceRunning(selectedInstanceId)
  const instance = selectedInstance()

  const [mode, setMode] = useState<Mode>('idle')
  const [sessionState, setSessionState] = useState<SessionState>('none')
  const [sessionCode, setSessionCode] = useState<string | null>(null)
  const [peerInput, setPeerInput] = useState('')
  const [copied, setCopied] = useState(false)
  const [statusMsg, setStatusMsg] = useState('')
  const [launchMsg, setLaunchMsg] = useState('')
  const peerInputRef = useRef<HTMLInputElement>(null)

  const sessionReady = sessionState === 'ready' || sessionState === 'connected'

  useEffect(() => {
    let unlistenState: (() => void) | null = null
    let unlistenError: (() => void) | null = null

    listen<{ running: boolean; instance_id: string }>('game_state', (event) => {
      const { running, instance_id } = event.payload
      setInstanceRunning(instance_id, running)
      if (!running) getCurrentWindow().show()
    }).then((fn) => { unlistenState = fn })

    listen<string>('launch_error', (event) => {
      setLaunchMsg(event.payload)
      if (selectedInstanceId) setInstanceRunning(selectedInstanceId, false)
    }).then((fn) => { unlistenError = fn })

    return () => {
      unlistenState?.()
      unlistenError?.()
    }
  }, [])

  const handleSelectMode = async (m: Mode) => {
    setMode(m)
    setStatusMsg('')
    setSessionCode(null)
    setPeerInput('')
    setSessionState('none')
    if (m === 'hosting') {
      await handleStartHost()
    }
  }

  const handleStartHost = async () => {
    setSessionState('starting')
    setStatusMsg('Initialisation du nœud P2P...')
    try {
      const code = await api.p2p.start()
      setSessionCode(code)
      setSessionState('ready')
      setStatusMsg('Partagez le code avec votre ami, puis lancez le jeu des deux côtés.')
    } catch (e) {
      setSessionState('none')
      setMode('idle')
      setStatusMsg(e instanceof Error ? e.message : 'Erreur de démarrage P2P')
    }
  }

  const handleJoin = async () => {
    const code = peerInput.trim()
    if (!code) return
    setSessionState('connecting')
    setStatusMsg('Connexion au pair...')
    try {
      await api.p2p.start()
      await api.p2p.join(code)
      setSessionState('connected')
      setStatusMsg('Connecté ! Lance le jeu des deux côtés maintenant.')
    } catch (e) {
      setSessionState('none')
      setStatusMsg(e instanceof Error ? e.message : 'Erreur de connexion P2P')
    }
  }

  const handleCopy = () => {
    if (!sessionCode) return
    navigator.clipboard.writeText(sessionCode)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  const handleLaunchP2p = async () => {
    if (!selectedInstanceId || gameRunning || !username) return
    setLaunchMsg('')
    try {
      if (mode === 'joining') {
        await api.launch.startP2pGuest(selectedInstanceId)
      } else {
        await api.launch.startP2p(selectedInstanceId)
      }
      setInstanceRunning(selectedInstanceId, true)
      if (closeOnLaunch) getCurrentWindow().hide()
    } catch (e) {
      setLaunchMsg(e instanceof Error ? e.message : 'Erreur de lancement')
    }
  }

  const canLaunch = !!selectedInstanceId && !!username && !gameRunning && sessionReady

  return (
    <div className="flex h-full flex-col items-center justify-center gap-6" style={{ background: '#09090D' }}>

      {/* Header */}
      <div className="flex flex-col items-center gap-2">
        <div className="flex items-center justify-center rounded-2xl" style={{ width: 56, height: 56, background: 'rgba(75,63,207,0.18)', border: '1px solid rgba(75,63,207,0.35)' }}>
          <svg viewBox="0 0 24 24" fill="rgba(120,110,230,0.9)" width={26} height={26}>
            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 14H9V8h2v8zm4 0h-2V8h2v8z"/>
          </svg>
        </div>
        <h1 className="font-black text-white" style={{ fontSize: 28, letterSpacing: '-0.01em', textShadow: '0 0 32px rgba(75,63,207,0.5)' }}>
          Serveur P2P
        </h1>
        <p style={{ fontSize: 13, color: 'rgba(255,255,255,0.3)', textAlign: 'center', maxWidth: 320 }}>
          {instance ? (
            <span style={{ color: 'rgba(120,110,230,0.7)', fontWeight: 600 }}>{instance.name} — {instance.mc_version}</span>
          ) : (
            <span style={{ color: 'rgba(255,100,100,0.6)' }}>Aucune instance sélectionnée</span>
          )}
        </p>
      </div>

      {/* Session zone */}
      <div style={{ width: 340, display: 'flex', flexDirection: 'column', gap: 12 }}>

        {/* Mode selection */}
        {mode === 'idle' && (
          <div style={{ display: 'flex', gap: 10 }}>
            <ModeCard
              icon={<IconHost />}
              label="Héberger"
              desc="Crée une session et partage le code"
              onClick={() => handleSelectMode('hosting')}
            />
            <ModeCard
              icon={<IconJoin />}
              label="Rejoindre"
              desc="Entrez le code de votre hôte"
              onClick={() => handleSelectMode('joining')}
            />
          </div>
        )}

        {/* Host flow */}
        {mode === 'hosting' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            <SessionHeader label="Mode Hôte" onReset={() => { setMode('idle'); setSessionState('none'); setSessionCode(null) }} />

            {sessionState === 'starting' && <Spinner label="Initialisation..." />}

            {(sessionState === 'ready') && sessionCode && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                <p style={{ fontSize: 11, color: 'rgba(255,255,255,0.35)', textAlign: 'center', marginBottom: 2 }}>
                  Code de session — partagez-le avec votre ami
                </p>
                <div style={{ display: 'flex', gap: 6, alignItems: 'center', background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(120,110,230,0.25)', borderRadius: 10, padding: '8px 12px' }}>
                  <span style={{ flex: 1, fontFamily: 'monospace', fontSize: 11, color: 'rgba(255,255,255,0.75)', wordBreak: 'break-all', lineHeight: 1.5 }}>{sessionCode}</span>
                  <button
                    onClick={handleCopy}
                    style={{ flexShrink: 0, padding: '4px 10px', borderRadius: 7, background: copied ? 'rgba(80,200,100,0.2)' : 'rgba(75,63,207,0.35)', border: `1px solid ${copied ? 'rgba(80,200,100,0.4)' : 'rgba(120,110,230,0.4)'}`, color: copied ? '#6ee87a' : 'rgba(200,195,255,0.9)', fontSize: 11, fontWeight: 700, cursor: 'pointer', transition: 'all 0.15s' }}
                  >
                    {copied ? '✓ Copié' : 'Copier'}
                  </button>
                </div>
              </div>
            )}
          </div>
        )}

        {/* Join flow */}
        {mode === 'joining' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            <SessionHeader label="Mode Rejoindre" onReset={() => { setMode('idle'); setSessionState('none'); setPeerInput('') }} />

            {(sessionState === 'none') && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                <p style={{ fontSize: 11, color: 'rgba(255,255,255,0.35)', textAlign: 'center' }}>
                  Code de session de l'hôte
                </p>
                <input
                  ref={peerInputRef}
                  value={peerInput}
                  onChange={(e) => setPeerInput(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && handleJoin()}
                  placeholder="12D3Koo..."
                  autoFocus
                  style={{ width: '100%', padding: '9px 13px', borderRadius: 10, background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(120,110,230,0.25)', color: 'rgba(255,255,255,0.8)', fontSize: 12, fontFamily: 'monospace', outline: 'none', boxSizing: 'border-box' }}
                />
                <button
                  onClick={handleJoin}
                  disabled={!peerInput.trim()}
                  style={{ padding: '10px', borderRadius: 10, background: peerInput.trim() ? 'rgba(75,63,207,0.7)' : 'rgba(60,55,90,0.4)', border: '1px solid rgba(120,110,230,0.3)', color: peerInput.trim() ? 'white' : 'rgba(255,255,255,0.3)', fontWeight: 700, fontSize: 13, cursor: peerInput.trim() ? 'pointer' : 'not-allowed', transition: 'all 0.15s', letterSpacing: '0.05em' }}
                >
                  SE CONNECTER
                </button>
              </div>
            )}

            {sessionState === 'connecting' && <Spinner label="Connexion au pair via relay..." />}

            {sessionState === 'connected' && (
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, background: 'rgba(80,200,100,0.08)', border: '1px solid rgba(80,200,100,0.25)', borderRadius: 10, padding: '10px 14px' }}>
                <span style={{ fontSize: 16 }}>🟢</span>
                <span style={{ fontSize: 12, color: 'rgba(150,230,160,0.9)', fontWeight: 600 }}>Connecté au pair</span>
              </div>
            )}
          </div>
        )}

        {/* Status message */}
        {statusMsg && (
          <p style={{ fontSize: 11, color: sessionState === 'connected' || sessionState === 'ready' ? 'rgba(160,230,170,0.8)' : 'rgba(255,255,255,0.35)', textAlign: 'center', lineHeight: 1.5, padding: '0 4px' }}>
            {statusMsg}
          </p>
        )}
      </div>

      {/* Launch button — visible uniquement si session prête */}
      {sessionReady && (
        <button
          onClick={username ? handleLaunchP2p : () => navigate('/login')}
          disabled={gameRunning || (!!username && !selectedInstanceId)}
          className="font-bold text-white transition-all duration-200 active:scale-95"
          style={{
            width: 280, height: 60, borderRadius: 18, fontSize: 14, letterSpacing: '0.06em',
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
            : 'LANCER LE JEU'}
        </button>
      )}

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

// ── Sub-components ────────────────────────────────────────────────────────────

function ModeCard({ icon, label, desc, onClick }: { icon: JSX.Element; label: string; desc: string; onClick: () => void }) {
  const [hov, setHov] = useState(false)
  return (
    <button
      onClick={onClick}
      onMouseEnter={() => setHov(true)}
      onMouseLeave={() => setHov(false)}
      style={{
        flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8, padding: '18px 12px',
        borderRadius: 14, cursor: 'pointer', transition: 'all 0.17s',
        background: hov ? 'rgba(75,63,207,0.18)' : 'rgba(255,255,255,0.03)',
        border: `1px solid ${hov ? 'rgba(120,110,230,0.5)' : 'rgba(255,255,255,0.08)'}`,
        boxShadow: hov ? '0 4px 24px rgba(75,63,207,0.2)' : 'none',
      }}
    >
      <span style={{ color: hov ? 'rgba(160,150,255,0.95)' : 'rgba(140,130,230,0.6)', transition: 'color 0.17s' }}>{icon}</span>
      <span style={{ fontSize: 13, fontWeight: 700, color: hov ? 'white' : 'rgba(255,255,255,0.7)', transition: 'color 0.17s' }}>{label}</span>
      <span style={{ fontSize: 10, color: 'rgba(255,255,255,0.3)', textAlign: 'center', lineHeight: 1.4 }}>{desc}</span>
    </button>
  )
}

function SessionHeader({ label, onReset }: { label: string; onReset: () => void }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 2 }}>
      <span style={{ fontSize: 12, fontWeight: 700, color: 'rgba(160,150,255,0.8)', letterSpacing: '0.04em' }}>{label.toUpperCase()}</span>
      <button
        onClick={onReset}
        style={{ fontSize: 10, color: 'rgba(255,255,255,0.25)', background: 'none', border: 'none', cursor: 'pointer', padding: '2px 6px' }}
        onMouseEnter={(e) => { (e.currentTarget as HTMLElement).style.color = 'rgba(255,255,255,0.6)' }}
        onMouseLeave={(e) => { (e.currentTarget as HTMLElement).style.color = 'rgba(255,255,255,0.25)' }}
      >
        Changer ↩
      </button>
    </div>
  )
}

function Spinner({ label }: { label: string }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8, padding: '14px 0' }}>
      <span className="h-4 w-4 animate-spin-slow rounded-full border-2" style={{ borderColor: 'rgba(120,110,230,0.2)', borderTopColor: 'rgba(120,110,230,0.8)' }} />
      <span style={{ fontSize: 12, color: 'rgba(255,255,255,0.4)' }}>{label}</span>
    </div>
  )
}

function IconHost() {
  return (
    <svg viewBox="0 0 24 24" fill="currentColor" width={22} height={22}>
      <path d="M12 2a5 5 0 1 1 0 10A5 5 0 0 1 12 2zm0 12c5.33 0 8 2.67 8 4v2H4v-2c0-1.33 2.67-4 8-4z" />
    </svg>
  )
}

function IconJoin() {
  return (
    <svg viewBox="0 0 24 24" fill="currentColor" width={22} height={22}>
      <path d="M15 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm-9-2V7H4v3H1v2h3v3h2v-3h3v-2H6zm9 4c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
    </svg>
  )
}
