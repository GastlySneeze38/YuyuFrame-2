import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '@/api/client'
import { useStore } from '@/stores/useStore'

type Step = 'idle' | 'loading' | 'code' | 'polling' | 'error'

export default function Login() {
  const navigate = useNavigate()
  const { setUser, theme } = useStore()
  const [step, setStep] = useState<Step>('idle')
  const [userCode, setUserCode] = useState('')
  const [verifyUrl, setVerifyUrl] = useState('')
  const [error, setError] = useState('')
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null)

  // Check if already authenticated
  useEffect(() => {
    api.auth.status().then((s) => {
      if (s.authenticated && s.username && s.uuid) {
        setUser(s.username, s.uuid)
        navigate('/home', { replace: true })
      }
    }).catch(() => {})
  }, [])

  const stopPolling = () => {
    if (pollRef.current) {
      clearInterval(pollRef.current)
      pollRef.current = null
    }
  }

  const startLogin = async () => {
    setStep('loading')
    setError('')
    try {
      const resp = await api.auth.startDevice()
      setUserCode(resp.user_code)
      setVerifyUrl(resp.verification_uri)
      setStep('code')

      // Open browser
      window.api?.openExternal(resp.verification_uri)

      // Start polling
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
        } catch {
          // network error, keep polling
        }
      }, 5000)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erreur de connexion au backend')
      setStep('error')
    }
  }

  useEffect(() => () => stopPolling(), [])

  const isGamer = theme === 'gamer'

  return (
    <div className="flex h-full flex-col items-center justify-center bg-bg-primary transition-theme">
      {/* Background glow */}
      <div
        className="pointer-events-none absolute inset-0"
        style={{
          background: isGamer
            ? 'radial-gradient(ellipse at 50% 60%, rgb(239 68 68 / 0.08) 0%, transparent 70%)'
            : 'radial-gradient(ellipse at 50% 60%, rgb(99 102 241 / 0.08) 0%, transparent 70%)',
        }}
      />

      <div className="relative z-10 flex w-full max-w-sm flex-col items-center gap-8 px-4">
        {/* Logo */}
        <div className="flex flex-col items-center gap-3">
          <div
            className="glow flex h-20 w-20 items-center justify-center rounded-2xl bg-accent text-4xl font-black text-white"
            style={{ fontFamily: 'monospace' }}
          >
            Y
          </div>
          <h1 className="glow-text text-3xl font-black tracking-widest text-txt-primary">
            YUYUFRAME
          </h1>
          <p className="text-sm text-txt-secondary">Minecraft Launcher</p>
        </div>

        {/* Card */}
        <div className="w-full rounded-2xl border border-border bg-bg-card p-6 transition-theme">
          {step === 'idle' && (
            <div className="flex flex-col gap-4">
              <p className="text-center text-sm text-txt-secondary">
                Connecte-toi avec ton compte Microsoft pour accéder à Minecraft.
              </p>
              <button
                onClick={startLogin}
                className="glow w-full rounded-xl bg-accent py-3 font-semibold text-white transition hover:bg-accent-hover active:scale-95"
              >
                Se connecter avec Microsoft
              </button>
            </div>
          )}

          {step === 'loading' && (
            <div className="flex flex-col items-center gap-3 py-4">
              <div className="h-8 w-8 animate-spin-slow rounded-full border-2 border-border border-t-accent" />
              <p className="text-sm text-txt-secondary">Connexion en cours...</p>
            </div>
          )}

          {(step === 'code' || step === 'polling') && (
            <div className="flex flex-col gap-4">
              <p className="text-center text-sm text-txt-secondary">
                Entre ce code sur la page Microsoft ouverte dans ton navigateur :
              </p>
              <div className="rounded-xl border border-border bg-bg-secondary py-3 text-center">
                <span className="font-mono text-2xl font-black tracking-[0.3em] text-accent">
                  {userCode}
                </span>
              </div>
              <button
                onClick={() => window.api?.openExternal(verifyUrl)}
                className="rounded-lg border border-border px-3 py-2 text-xs text-txt-secondary transition hover:border-accent hover:text-accent"
              >
                Ouvrir {verifyUrl}
              </button>
              {step === 'polling' && (
                <div className="flex items-center justify-center gap-2 text-xs text-txt-secondary">
                  <div className="h-3 w-3 animate-spin-slow rounded-full border border-border border-t-accent" />
                  En attente de confirmation...
                </div>
              )}
            </div>
          )}

          {step === 'error' && (
            <div className="flex flex-col gap-4">
              <p className="rounded-lg bg-red-900/20 px-3 py-2 text-center text-sm text-red-400">
                {error}
              </p>
              <button
                onClick={() => setStep('idle')}
                className="w-full rounded-xl border border-border py-2 text-sm text-txt-secondary transition hover:border-accent hover:text-accent"
              >
                Réessayer
              </button>
            </div>
          )}
        </div>

        {/* Theme indicator */}
        <p className="text-xs text-txt-secondary opacity-40">
          Thème {theme === 'chill' ? 'Chill ❄️' : 'Gamer 🎮'} — toggle dans la barre de titre
        </p>
      </div>
    </div>
  )
}
