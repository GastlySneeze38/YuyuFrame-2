import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '@/api/client'
import { useStore } from '@/stores/useStore'
import { Background, useChillPalette } from '@/components/Background'

type Step = 'idle' | 'loading' | 'code' | 'polling' | 'error'

export default function Login() {
  const navigate = useNavigate()
  const { setUser, theme } = useStore()
  const [step, setStep] = useState<Step>('idle')
  const [userCode, setUserCode] = useState('')
  const [verifyUrl, setVerifyUrl] = useState('')
  const [error, setError] = useState('')
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null)
  const palette = useChillPalette()

  useEffect(() => {
    api.auth.status().then((s) => {
      if (s.authenticated && s.username && s.uuid) {
        setUser(s.username, s.uuid)
        navigate('/home', { replace: true })
      }
    }).catch(() => {})
  }, [])

  const stopPolling = () => {
    if (pollRef.current) { clearInterval(pollRef.current); pollRef.current = null }
  }

  const startLogin = async () => {
    setStep('loading')
    setError('')
    try {
      const resp = await api.auth.startDevice()
      setUserCode(resp.user_code)
      setVerifyUrl(resp.verification_uri)
      window.api?.openExternal(resp.verification_uri)
      setStep('polling')
      pollRef.current = setInterval(async () => {
        try {
          const poll = await api.auth.poll()
          if (poll.status === 'success' && poll.username) {
            stopPolling()
            const status = await api.auth.status()
            if (status.uuid) {
              setUser(poll.username, status.uuid)
              navigate('/home', { replace: true })
            }
          } else if (poll.status === 'error') {
            stopPolling()
            setError(poll.error ?? 'Erreur inconnue')
            setStep('error')
          }
        } catch { /* keep polling */ }
      }, 5000)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erreur de connexion au backend')
      setStep('error')
    }
  }

  useEffect(() => () => stopPolling(), [])

  const isGamer = theme === 'gamer'
  // Card bg: same as the controls panel in Home
  const cardBg = isGamer ? 'rgb(40,40,40)' : palette.s2
  const cardBorder = isGamer ? 'rgb(68,62,185)' : 'rgba(0,0,0,0.2)'

  return (
    <div className="relative flex h-full items-center justify-center overflow-hidden">
      {/* Full-screen themed background */}
      <Background />

      {/* Centered login card — matches original GamerLoginPanel card style */}
      <div
        className="relative z-10 flex w-80 flex-col items-center gap-5 rounded-2xl p-6"
        style={{
          background: cardBg,
          border: `1px solid ${cardBorder}`,
          boxShadow: isGamer
            ? '0 0 40px rgb(68 62 185 / 0.2), 0 8px 32px rgba(0,0,0,0.6)'
            : '0 8px 32px rgba(0,0,0,0.4)',
        }}
      >
        {/* Player avatar placeholder (150x150 in original) */}
        <div
          className="flex h-[120px] w-[120px] items-center justify-center rounded-2xl"
          style={{
            background: isGamer ? 'rgb(60,60,60)' : 'rgba(0,0,0,0.2)',
            border: '2px dashed rgba(255,255,255,0.15)',
          }}
        >
          {step === 'polling' || step === 'loading' ? (
            <div
              className="h-8 w-8 animate-spin-slow rounded-full border-2"
              style={{ borderColor: 'rgba(255,255,255,0.15)', borderTopColor: 'rgb(68,62,185)' }}
            />
          ) : (
            <svg viewBox="0 0 24 24" fill="rgba(255,255,255,0.2)" className="h-16 w-16">
              <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
            </svg>
          )}
        </div>

        {/* Title */}
        <div className="text-center">
          <h1
            className="font-black text-2xl tracking-[0.2em] text-white"
            style={{
              fontFamily: 'monospace',
              textShadow: isGamer ? '0 0 16px rgb(68 62 185 / 0.8)' : 'none',
            }}
          >
            YUYUFRAME
          </h1>
          <p className="mt-0.5 text-[10px] tracking-widest text-white/40 uppercase">
            Connexion au compte Microsoft
          </p>
        </div>

        {/* Divider */}
        <div className="w-full h-px" style={{ background: 'rgba(255,255,255,0.1)' }} />

        {/* Content by step */}
        <div className="w-full">
          {step === 'idle' && (
            <div className="flex flex-col gap-3">
              <p className="text-center text-xs text-white/50">
                Connecte-toi avec ton compte Microsoft pour accéder à Minecraft.
              </p>
              {/* Login button — 100x100 style in original, we adapt to full-width */}
              <button
                onClick={startLogin}
                className="flex w-full items-center justify-center gap-3 rounded-xl py-3.5 font-bold text-white transition-all active:scale-95"
                style={{
                  background: 'rgb(68,62,185)',
                  boxShadow: '0 4px 16px rgb(68 62 185 / 0.4)',
                }}
                onMouseEnter={(e) => { e.currentTarget.style.background = 'rgb(88,82,220)' }}
                onMouseLeave={(e) => { e.currentTarget.style.background = 'rgb(68,62,185)' }}
              >
                {/* Windows logo */}
                <svg className="h-5 w-5" viewBox="0 0 88 88" fill="currentColor">
                  <path d="M0 12.402l35.687-4.86.016 34.423-35.67.203zm35.67 33.529l.028 34.453L.028 75.48l-.01-29.978zm4.326-39.025L87.314 0v41.527l-47.318.376zm47.329 39.349l-.015 41.344-47.318-6.532-.066-34.847z" />
                </svg>
                Se connecter avec Microsoft
              </button>
            </div>
          )}

          {step === 'loading' && (
            <div className="flex flex-col items-center gap-2 py-2">
              <p className="text-xs text-white/50">Connexion en cours...</p>
            </div>
          )}

          {step === 'polling' && (
            <div className="flex flex-col gap-3">
              <p className="text-center text-xs text-white/50">
                Entre ce code sur la page Microsoft :
              </p>
              <div
                className="rounded-xl py-4 text-center"
                style={{ background: 'rgba(0,0,0,0.35)', border: '1px solid rgba(255,255,255,0.1)' }}
              >
                <span className="font-mono text-2xl font-black tracking-[0.3em] text-white">
                  {userCode}
                </span>
              </div>
              <button
                onClick={() => window.api?.openExternal(verifyUrl)}
                className="rounded-lg py-2 text-xs transition"
                style={{
                  border: '1px solid rgba(255,255,255,0.1)',
                  color: 'rgba(255,255,255,0.4)',
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.borderColor = 'rgb(68,62,185)'
                  e.currentTarget.style.color = 'rgb(68,62,185)'
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.borderColor = 'rgba(255,255,255,0.1)'
                  e.currentTarget.style.color = 'rgba(255,255,255,0.4)'
                }}
              >
                Ouvrir la page Microsoft →
              </button>
              <div className="flex items-center justify-center gap-2 text-xs text-white/40">
                <span
                  className="h-3 w-3 animate-spin-slow rounded-full border"
                  style={{ borderColor: 'rgba(255,255,255,0.15)', borderTopColor: 'rgb(68,62,185)' }}
                />
                En attente de confirmation...
              </div>
            </div>
          )}

          {step === 'error' && (
            <div className="flex flex-col gap-3">
              <div
                className="rounded-xl px-3 py-2 text-center text-xs text-red-300"
                style={{ background: 'rgba(200,50,50,0.2)' }}
              >
                {error}
              </div>
              <button
                onClick={() => setStep('idle')}
                className="w-full rounded-xl py-2.5 text-sm text-white/60 transition"
                style={{ border: '1px solid rgba(255,255,255,0.1)' }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.borderColor = 'rgb(68,62,185)'
                  e.currentTarget.style.color = 'rgb(68,62,185)'
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.borderColor = 'rgba(255,255,255,0.1)'
                  e.currentTarget.style.color = 'rgba(255,255,255,0.6)'
                }}
              >
                Réessayer
              </button>
            </div>
          )}
        </div>

        {/* Back to home link */}
        <button
          onClick={() => navigate('/home')}
          className="text-[10px] text-white/25 transition hover:text-white/50"
        >
          ← Retour sans se connecter
        </button>
      </div>
    </div>
  )
}
