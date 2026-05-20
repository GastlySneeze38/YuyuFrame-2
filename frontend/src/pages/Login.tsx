import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { open } from '@tauri-apps/plugin-shell'
import { api } from '@/api/client'
import { useStore } from '@/stores/useStore'
import type { Account } from '@/types'

type Step = 'idle' | 'loading' | 'polling' | 'error'

export default function Login() {
  const navigate = useNavigate()
  const { uuid, accounts, setAccounts, setUser, removeAccount } = useStore()
  const [step, setStep] = useState<Step>('idle')
  const [userCode, setUserCode] = useState('')
  const [verifyUrl, setVerifyUrl] = useState('')
  const [error, setError] = useState('')
  const [copied, setCopied] = useState(false)
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null)

  // Sync accounts from backend on mount
  useEffect(() => {
    api.mc.accounts()
      .then((accs) => {
        const mapped: Account[] = accs.map((a) => ({ username: a.mc_username, uuid: a.mc_uuid }))
        setAccounts(mapped)
        const active = accs.find((a) => a.is_active)
        if (active) setUser(active.mc_username, active.mc_uuid)
      })
      .catch(() => {})
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
      open(resp.verification_uri)
      setStep('polling')
      pollRef.current = setInterval(async () => {
        try {
          const poll = await api.auth.poll()
          if (poll.status === 'success' && poll.username) {
            stopPolling()
            // Refresh account list from backend (backend already stored the session)
            const accs = await api.mc.accounts()
            const mapped: Account[] = accs.map((a) => ({ username: a.mc_username, uuid: a.mc_uuid }))
            setAccounts(mapped)
            const active = accs.find((a) => a.is_active)
            if (active) setUser(active.mc_username, active.mc_uuid)
            navigate('/home', { replace: true })
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

  const handleSelect = async (acc: Account) => {
    try {
      await api.mc.switch(acc.uuid)
      setUser(acc.username, acc.uuid)
      navigate('/home')
    } catch { /* ignore */ }
  }

  const handleRemove = async (acc: Account) => {
    try {
      await api.mc.delete(acc.uuid)
      removeAccount(acc.uuid)
    } catch { /* ignore */ }
  }

  useEffect(() => () => stopPolling(), [])

  return (
    <div className="flex h-full flex-col overflow-hidden" style={{ background: '#09090D' }}>

      {/* Header */}
      <div
        className="flex flex-shrink-0 items-center gap-3 px-6 py-3"
        style={{ borderBottom: '1px solid rgba(255,255,255,0.06)' }}
      >
        <button
          onClick={() => navigate('/home')}
          className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-lg transition-all duration-150"
          style={{ color: 'rgba(255,255,255,0.35)', background: 'rgba(255,255,255,0.04)' }}
          onMouseEnter={(e) => {
            e.currentTarget.style.color = 'rgba(255,255,255,0.7)'
            e.currentTarget.style.background = 'rgba(255,255,255,0.08)'
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.color = 'rgba(255,255,255,0.35)'
            e.currentTarget.style.background = 'rgba(255,255,255,0.04)'
          }}
        >
          <svg viewBox="0 0 24 24" fill="currentColor" style={{ width: 15, height: 15 }}>
            <path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z" />
          </svg>
        </button>
        <div style={{ width: 1, height: 28, background: 'rgba(255,255,255,0.07)', flexShrink: 0 }} />
        <div>
          <h1 className="font-black text-white" style={{ fontSize: 16, letterSpacing: '-0.01em', lineHeight: 1.2 }}>
            Connexion
          </h1>
          <p style={{ fontSize: 10, color: 'rgba(255,255,255,0.28)', marginTop: 1 }}>
            Gérez vos comptes Minecraft (max 2)
          </p>
        </div>
      </div>

      {/* Content */}
      <div className="flex flex-1 flex-col items-center justify-center gap-8 overflow-y-auto p-8">

        {/* Branding */}
        <div className="flex flex-col items-center gap-1">
          <div className="flex items-center gap-2">
            <div className="h-4 w-4 rounded-sm" style={{ background: '#4B3FCF' }} />
            <span className="font-black text-white" style={{ fontSize: 22, letterSpacing: '-0.01em' }}>
              YuyuFrame
            </span>
          </div>
          <p style={{ fontSize: 11, color: 'rgba(255,255,255,0.28)' }}>
            {accounts.length === 0
              ? 'Connecte-toi pour jouer'
              : accounts.length < 2
              ? 'Ajoute un deuxième compte ou continue'
              : 'Sélectionne le compte avec lequel jouer'}
          </p>
        </div>

        {/* Account slots */}
        <div className="flex flex-wrap items-start justify-center gap-4">
          {accounts.map((acc) => (
            <AccountCard
              key={acc.uuid}
              acc={acc}
              isActive={acc.uuid === uuid}
              onSelect={() => handleSelect(acc)}
              onRemove={() => handleRemove(acc)}
            />
          ))}

          {accounts.length < 2 && step === 'idle' && (
            <AddCard onClick={startLogin} />
          )}
        </div>

        {/* Auth flow panel */}
        {(step === 'loading' || step === 'polling' || step === 'error') && (
          <div
            className="w-full max-w-sm rounded-2xl p-6"
            style={{
              background: 'rgba(255,255,255,0.025)',
              border: '1px solid rgba(255,255,255,0.07)',
            }}
          >
            {step === 'loading' && (
              <div className="flex items-center justify-center gap-3 py-3">
                <span
                  className="h-4 w-4 animate-spin-slow rounded-full border-2"
                  style={{ borderColor: 'rgba(255,255,255,0.15)', borderTopColor: '#4B3FCF' }}
                />
                <span style={{ fontSize: 13, color: 'rgba(255,255,255,0.5)' }}>Connexion en cours...</span>
              </div>
            )}

            {step === 'polling' && (
              <div className="flex flex-col gap-4">
                <p className="text-center" style={{ fontSize: 12, color: 'rgba(255,255,255,0.45)' }}>
                  Entre ce code sur la page Microsoft :
                </p>
                <button
                  onClick={() => { navigator.clipboard.writeText(userCode); setCopied(true); setTimeout(() => setCopied(false), 2000) }}
                  className="rounded-xl py-4 text-center transition-all duration-150"
                  style={{ background: copied ? 'rgba(74,222,128,0.08)' : 'rgba(0,0,0,0.4)', border: `1px solid ${copied ? 'rgba(74,222,128,0.3)' : 'rgba(255,255,255,0.08)'}` }}
                  title="Cliquer pour copier"
                >
                  <span className="font-mono font-black text-white" style={{ fontSize: 28, letterSpacing: '0.25em' }}>
                    {userCode}
                  </span>
                  <p style={{ fontSize: 10, marginTop: 4, color: copied ? 'rgb(134,239,172)' : 'rgba(255,255,255,0.2)' }}>
                    {copied ? 'Copié !' : 'Cliquer pour copier'}
                  </p>
                </button>
                <div className="flex gap-2">
                  <button
                    onClick={() => open(verifyUrl)}
                    className="flex-1 rounded-xl py-2.5 text-sm font-medium text-white transition-all duration-150"
                    style={{ background: 'rgba(75,63,207,0.15)', border: '1px solid rgba(75,63,207,0.3)' }}
                    onMouseEnter={(e) => { e.currentTarget.style.background = 'rgba(75,63,207,0.3)' }}
                    onMouseLeave={(e) => { e.currentTarget.style.background = 'rgba(75,63,207,0.15)' }}
                  >
                    Ouvrir Microsoft →
                  </button>
                  <button
                    onClick={() => { stopPolling(); setStep('idle') }}
                    className="rounded-xl px-4 py-2.5 text-sm transition-all duration-150"
                    style={{ color: 'rgba(255,255,255,0.3)', border: '1px solid rgba(255,255,255,0.07)' }}
                    onMouseEnter={(e) => { e.currentTarget.style.color = 'rgba(255,255,255,0.65)' }}
                    onMouseLeave={(e) => { e.currentTarget.style.color = 'rgba(255,255,255,0.3)' }}
                  >
                    Annuler
                  </button>
                </div>
                <div className="flex items-center justify-center gap-2">
                  <span
                    className="h-3 w-3 animate-spin-slow rounded-full border"
                    style={{ borderColor: 'rgba(255,255,255,0.12)', borderTopColor: '#4B3FCF' }}
                  />
                  <span style={{ fontSize: 11, color: 'rgba(255,255,255,0.35)' }}>
                    En attente de confirmation...
                  </span>
                </div>
              </div>
            )}

            {step === 'error' && (
              <div className="flex flex-col gap-3">
                <div
                  className="rounded-xl px-4 py-3"
                  style={{ background: 'rgba(200,50,50,0.12)', border: '1px solid rgba(200,50,50,0.2)' }}
                >
                  <p style={{ fontSize: 11, color: 'rgb(252,165,165)', wordBreak: 'break-all', whiteSpace: 'pre-wrap' }}>{error}</p>
                </div>
                <div className="flex gap-2">
                  <button
                    onClick={() => { navigator.clipboard.writeText(error); setCopied(true); setTimeout(() => setCopied(false), 2000) }}
                    className="rounded-xl px-3 py-2.5 text-sm transition-all duration-150"
                    style={{ color: copied ? 'rgb(134,239,172)' : 'rgba(255,255,255,0.4)', border: `1px solid ${copied ? 'rgba(74,222,128,0.3)' : 'rgba(255,255,255,0.08)'}` }}
                  >
                    {copied ? 'Copié ✓' : 'Copier'}
                  </button>
                  <button
                    onClick={() => setStep('idle')}
                    className="flex-1 rounded-xl py-2.5 text-sm transition-all duration-150"
                    style={{ color: 'rgba(255,255,255,0.4)', border: '1px solid rgba(255,255,255,0.08)' }}
                    onMouseEnter={(e) => { e.currentTarget.style.borderColor = 'rgba(75,63,207,0.4)'; e.currentTarget.style.color = 'rgba(255,255,255,0.7)' }}
                    onMouseLeave={(e) => { e.currentTarget.style.borderColor = 'rgba(255,255,255,0.08)'; e.currentTarget.style.color = 'rgba(255,255,255,0.4)' }}
                  >
                    Réessayer
                  </button>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  )
}

function AccountCard({
  acc, isActive, onSelect, onRemove,
}: {
  acc: Account
  isActive: boolean
  onSelect: () => void
  onRemove: () => void
}) {
  return (
    <div
      className="relative flex w-48 flex-col items-center gap-4 rounded-2xl p-5"
      style={{
        background: isActive ? 'rgba(75,63,207,0.08)' : 'rgba(255,255,255,0.025)',
        border: `1px solid ${isActive ? 'rgba(75,63,207,0.4)' : 'rgba(255,255,255,0.07)'}`,
        boxShadow: isActive ? '0 0 32px rgba(75,63,207,0.14)' : 'none',
      }}
    >
      <div className="relative mt-2">
        <img
          src={`https://mc-heads.net/avatar/${acc.uuid}/80`}
          alt={acc.username}
          className="rounded-xl"
          style={{ width: 80, height: 80, imageRendering: 'pixelated' }}
          onError={(e) => {
            e.currentTarget.style.display = 'none'
            const fb = e.currentTarget.nextElementSibling as HTMLElement | null
            if (fb) fb.style.display = 'flex'
          }}
        />
        <div
          className="hidden items-center justify-center rounded-xl font-black text-white"
          style={{ width: 80, height: 80, background: 'rgba(75,63,207,0.45)', fontFamily: 'monospace', fontSize: 28 }}
        >
          {acc.username[0].toUpperCase()}
        </div>
        {isActive && (
          <div
            className="absolute -bottom-1 -right-1 h-4 w-4 rounded-full border-2"
            style={{ background: '#22c55e', borderColor: '#09090D' }}
          />
        )}
      </div>

      <div className="text-center">
        <p className="font-bold text-white" style={{ fontSize: 14 }}>{acc.username}</p>
        <p style={{ fontSize: 10, marginTop: 2, color: isActive ? 'rgba(74,222,128,0.7)' : 'rgba(255,255,255,0.25)' }}>
          {isActive ? '● Actif' : 'Compte sauvegardé'}
        </p>
      </div>

      <div className="flex w-full flex-col gap-1.5">
        {isActive ? (
          <button
            onClick={onSelect}
            className="w-full rounded-xl py-2 text-sm font-medium text-white transition-all duration-150"
            style={{ background: 'rgba(75,63,207,0.25)', border: '1px solid rgba(75,63,207,0.45)' }}
            onMouseEnter={(e) => { e.currentTarget.style.background = 'rgba(75,63,207,0.42)' }}
            onMouseLeave={(e) => { e.currentTarget.style.background = 'rgba(75,63,207,0.25)' }}
          >
            Jouer →
          </button>
        ) : (
          <button
            onClick={onSelect}
            className="w-full rounded-xl py-2 text-sm transition-all duration-150"
            style={{ color: 'rgba(255,255,255,0.45)', border: '1px solid rgba(255,255,255,0.08)' }}
            onMouseEnter={(e) => { e.currentTarget.style.borderColor = 'rgba(75,63,207,0.45)'; e.currentTarget.style.color = 'rgba(255,255,255,0.9)' }}
            onMouseLeave={(e) => { e.currentTarget.style.borderColor = 'rgba(255,255,255,0.08)'; e.currentTarget.style.color = 'rgba(255,255,255,0.45)' }}
          >
            Sélectionner
          </button>
        )}
        <button
          onClick={onRemove}
          className="w-full rounded-xl py-2 text-sm transition-all duration-150"
          style={{ color: 'rgba(255,255,255,0.25)', border: '1px solid rgba(255,255,255,0.05)' }}
          onMouseEnter={(e) => { e.currentTarget.style.color = 'rgb(252,165,165)'; e.currentTarget.style.borderColor = 'rgba(200,50,50,0.3)' }}
          onMouseLeave={(e) => { e.currentTarget.style.color = 'rgba(255,255,255,0.25)'; e.currentTarget.style.borderColor = 'rgba(255,255,255,0.05)' }}
        >
          Déconnecter
        </button>
      </div>
    </div>
  )
}

function AddCard({ onClick }: { onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      className="flex w-48 flex-col items-center justify-center gap-4 rounded-2xl transition-all duration-200"
      style={{
        height: 258,
        background: 'rgba(255,255,255,0.015)',
        border: '2px dashed rgba(255,255,255,0.08)',
        color: 'rgba(255,255,255,0.25)',
      }}
      onMouseEnter={(e) => {
        e.currentTarget.style.borderColor = 'rgba(75,63,207,0.45)'
        e.currentTarget.style.color = 'rgba(140,130,240,0.75)'
        e.currentTarget.style.background = 'rgba(75,63,207,0.05)'
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.borderColor = 'rgba(255,255,255,0.08)'
        e.currentTarget.style.color = 'rgba(255,255,255,0.25)'
        e.currentTarget.style.background = 'rgba(255,255,255,0.015)'
      }}
    >
      <div
        className="flex h-14 w-14 items-center justify-center rounded-2xl"
        style={{ background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.06)' }}
      >
        <svg className="h-7 w-7" viewBox="0 0 88 88" fill="currentColor">
          <path d="M0 12.402l35.687-4.86.016 34.423-35.67.203zm35.67 33.529l.028 34.453L.028 75.48l-.01-29.978zm4.326-39.025L87.314 0v41.527l-47.318.376zm47.329 39.349l-.015 41.344-47.318-6.532-.066-34.847z" />
        </svg>
      </div>
      <div className="text-center">
        <p style={{ fontSize: 13, fontWeight: 600 }}>Ajouter un compte</p>
        <p style={{ fontSize: 10, marginTop: 4, color: 'rgba(255,255,255,0.2)' }}>Connexion via Microsoft</p>
      </div>
    </button>
  )
}
