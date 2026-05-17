import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '@/api/client'
import { useStore } from '@/stores/useStore'
import type { Account } from '@/types'

type Step = 'idle' | 'loading' | 'polling' | 'error'

export default function Login() {
  const navigate = useNavigate()
  const { uuid, accounts, addAccount, removeAccount, switchAccount } = useStore()
  const [step, setStep] = useState<Step>('idle')
  const [userCode, setUserCode] = useState('')
  const [verifyUrl, setVerifyUrl] = useState('')
  const [error, setError] = useState('')
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null)

  useEffect(() => {
    api.auth.status().then((s) => {
      if (s.authenticated && s.username && s.uuid) {
        addAccount(s.username, s.uuid)
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
              addAccount(poll.username, status.uuid)
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

  const handleSelect = (acc: Account) => {
    switchAccount(acc.uuid)
    navigate('/home')
  }

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
            Gérez vos comptes Minecraft
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
              onRemove={() => removeAccount(acc.uuid)}
            />
          ))}

          {/* Add slot: visible only when < 2 accounts and not in polling */}
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
                <span style={{ fontSize: 13, color: 'rgba(255,255,255,0.5)' }}>
                  Connexion en cours...
                </span>
              </div>
            )}

            {step === 'polling' && (
              <div className="flex flex-col gap-4">
                <p className="text-center" style={{ fontSize: 12, color: 'rgba(255,255,255,0.45)' }}>
                  Entre ce code sur la page Microsoft :
                </p>
                <div
                  className="rounded-xl py-4 text-center"
                  style={{ background: 'rgba(0,0,0,0.4)', border: '1px solid rgba(255,255,255,0.08)' }}
                >
                  <span
                    className="font-mono font-black text-white"
                    style={{ fontSize: 28, letterSpacing: '0.25em' }}
                  >
                    {userCode}
                  </span>
                </div>
                <div className="flex gap-2">
                  <button
                    onClick={() => window.api?.openExternal(verifyUrl)}
                    className="flex-1 rounded-xl py-2.5 text-sm font-medium text-white transition-all duration-150"
                    style={{
                      background: 'rgba(75,63,207,0.15)',
                      border: '1px solid rgba(75,63,207,0.3)',
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.background = 'rgba(75,63,207,0.3)'
                      e.currentTarget.style.borderColor = 'rgba(75,63,207,0.55)'
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.background = 'rgba(75,63,207,0.15)'
                      e.currentTarget.style.borderColor = 'rgba(75,63,207,0.3)'
                    }}
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
                  className="rounded-xl px-4 py-3 text-center"
                  style={{ background: 'rgba(200,50,50,0.12)', border: '1px solid rgba(200,50,50,0.2)' }}
                >
                  <span style={{ fontSize: 12, color: 'rgb(252,165,165)' }}>{error}</span>
                </div>
                <button
                  onClick={() => setStep('idle')}
                  className="w-full rounded-xl py-2.5 text-sm transition-all duration-150"
                  style={{ color: 'rgba(255,255,255,0.4)', border: '1px solid rgba(255,255,255,0.08)' }}
                  onMouseEnter={(e) => {
                    e.currentTarget.style.borderColor = 'rgba(75,63,207,0.4)'
                    e.currentTarget.style.color = 'rgba(255,255,255,0.7)'
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.borderColor = 'rgba(255,255,255,0.08)'
                    e.currentTarget.style.color = 'rgba(255,255,255,0.4)'
                  }}
                >
                  Réessayer
                </button>
              </div>
            )}
          </div>
        )}

        {/* Add account button shown below when user has accounts but not in add-flow */}
        {accounts.length > 0 && accounts.length < 2 && step !== 'idle' && (
          <button
            onClick={() => setStep('idle')}
            style={{ fontSize: 11, color: 'rgba(255,255,255,0.2)' }}
            className="transition-colors duration-150"
            onMouseEnter={(e) => { (e.currentTarget as HTMLElement).style.color = 'rgba(255,255,255,0.5)' }}
            onMouseLeave={(e) => { (e.currentTarget as HTMLElement).style.color = 'rgba(255,255,255,0.2)' }}
          >
            ← Annuler l'ajout de compte
          </button>
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
      {/* Remove */}
      <button
        onClick={onRemove}
        className="absolute right-3 top-3 flex h-6 w-6 items-center justify-center rounded-lg transition-all duration-150"
        style={{ color: 'rgba(255,255,255,0.18)', background: 'rgba(255,255,255,0.04)' }}
        onMouseEnter={(e) => {
          e.currentTarget.style.color = 'rgba(252,165,165,0.85)'
          e.currentTarget.style.background = 'rgba(200,50,50,0.14)'
        }}
        onMouseLeave={(e) => {
          e.currentTarget.style.color = 'rgba(255,255,255,0.18)'
          e.currentTarget.style.background = 'rgba(255,255,255,0.04)'
        }}
      >
        <svg viewBox="0 0 24 24" fill="currentColor" style={{ width: 11, height: 11 }}>
          <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z" />
        </svg>
      </button>

      {/* Avatar */}
      <div className="relative mt-2">
        <img
          src={`https://crafatar.com/avatars/${acc.uuid}?size=80&overlay`}
          alt={acc.username}
          className="rounded-xl"
          style={{ width: 80, height: 80, imageRendering: 'pixelated' }}
          onError={(e) => {
            const el = e.currentTarget
            el.style.display = 'none'
            const fallback = el.nextElementSibling as HTMLElement | null
            if (fallback) fallback.style.display = 'flex'
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

      {/* Name */}
      <div className="text-center">
        <p className="font-bold text-white" style={{ fontSize: 14 }}>{acc.username}</p>
        <p style={{ fontSize: 10, marginTop: 2, color: isActive ? 'rgba(74,222,128,0.7)' : 'rgba(255,255,255,0.25)' }}>
          {isActive ? '● Actif' : 'Compte sauvegardé'}
        </p>
      </div>

      {/* Action */}
      {isActive ? (
        <button
          onClick={onSelect}
          className="w-full rounded-xl py-2 text-sm font-medium text-white transition-all duration-150"
          style={{
            background: 'rgba(75,63,207,0.25)',
            border: '1px solid rgba(75,63,207,0.45)',
          }}
          onMouseEnter={(e) => {
            e.currentTarget.style.background = 'rgba(75,63,207,0.42)'
            e.currentTarget.style.borderColor = 'rgba(75,63,207,0.7)'
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.background = 'rgba(75,63,207,0.25)'
            e.currentTarget.style.borderColor = 'rgba(75,63,207,0.45)'
          }}
        >
          Jouer →
        </button>
      ) : (
        <button
          onClick={onSelect}
          className="w-full rounded-xl py-2 text-sm transition-all duration-150"
          style={{ color: 'rgba(255,255,255,0.45)', border: '1px solid rgba(255,255,255,0.08)' }}
          onMouseEnter={(e) => {
            e.currentTarget.style.borderColor = 'rgba(75,63,207,0.45)'
            e.currentTarget.style.color = 'rgba(255,255,255,0.9)'
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.borderColor = 'rgba(255,255,255,0.08)'
            e.currentTarget.style.color = 'rgba(255,255,255,0.45)'
          }}
        >
          Sélectionner
        </button>
      )}
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
