import { useNavigate } from 'react-router-dom'
import { useStore } from '@/stores/useStore'

const PLANS = [
  {
    id: 'free',
    name: 'Free',
    price: null,
    color: 'rgba(255,255,255,0.12)',
    borderColor: 'rgba(255,255,255,0.1)',
    badgeBg: 'rgba(255,255,255,0.08)',
    badgeColor: 'rgba(255,255,255,0.5)',
    glowColor: 'rgba(255,255,255,0.03)',
    features: [
      { label: 'Launcher Minecraft complet', ok: true },
      { label: 'Instances illimitées (locales)', ok: true },
      { label: '2 comptes Minecraft', ok: true },
      { label: 'Gestion des mods', ok: true },
      { label: 'Console de jeu', ok: true },
      { label: 'Sync cloud', ok: false },
      { label: 'Sauvegarde cloud', ok: false },
      { label: 'Support prioritaire', ok: false },
    ],
  },
  {
    id: 'premium',
    name: 'Premium',
    price: '7.99',
    color: '#818cf8',
    borderColor: 'rgba(129,140,248,0.35)',
    badgeBg: 'rgba(75,63,207,0.25)',
    badgeColor: '#818cf8',
    glowColor: 'rgba(75,63,207,0.08)',
    features: [
      { label: 'Tout ce qui est inclus dans Free', ok: true },
      { label: 'Sync cloud (jusqu\'à 3 saves)', ok: true },
      { label: 'Jusqu\'à 4 instances synchronisées', ok: true },
      { label: 'Restauration cloud en un clic', ok: true },
      { label: 'Support prioritaire', ok: true },
      { label: 'Sync 10 saves', ok: false },
      { label: '10 instances synchronisées', ok: false },
    ],
  },
  {
    id: 'ultimate',
    name: 'Ultimate',
    price: '15.99',
    color: '#f59e0b',
    borderColor: 'rgba(245,158,11,0.35)',
    badgeBg: 'rgba(245,158,11,0.15)',
    badgeColor: '#f59e0b',
    glowColor: 'rgba(245,158,11,0.06)',
    features: [
      { label: 'Tout ce qui est inclus dans Premium', ok: true },
      { label: 'Sync cloud (jusqu\'à 10 saves)', ok: true },
      { label: 'Jusqu\'à 10 instances synchronisées', ok: true },
      { label: 'Restauration cloud en un clic', ok: true },
      { label: 'Support prioritaire', ok: true },
      { label: 'Accès anticipé aux nouvelles fonctionnalités', ok: true },
    ],
  },
]

export default function Plans() {
  const navigate = useNavigate()
  const { yuyuPlan, yuyuUsername, isPremium, isUltimate } = useStore()

  const effectivePlan = isUltimate() ? 'ultimate' : isPremium() ? 'premium' : 'free'

  return (
    <div
      className="flex h-full flex-col overflow-auto"
      style={{ background: '#09090D', color: 'white' }}
    >
      <div className="mx-auto w-full max-w-5xl px-6 py-10 flex flex-col gap-10">

        {/* Header */}
        <div className="flex items-center justify-between">
          <button
            onClick={() => navigate('/home')}
            className="flex items-center gap-2 transition-colors duration-150"
            style={{ fontSize: 12, color: 'rgba(255,255,255,0.3)', fontWeight: 500 }}
            onMouseEnter={(e) => { (e.currentTarget as HTMLElement).style.color = 'rgba(255,255,255,0.7)' }}
            onMouseLeave={(e) => { (e.currentTarget as HTMLElement).style.color = 'rgba(255,255,255,0.3)' }}
          >
            <svg viewBox="0 0 24 24" fill="currentColor" width={14} height={14}>
              <path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z" />
            </svg>
            Retour
          </button>

          <div className="flex flex-col items-center gap-1">
            <h1 className="font-black text-white" style={{ fontSize: 30, letterSpacing: '-0.02em', textShadow: '0 0 40px rgba(75,63,207,0.5)' }}>
              Plans YuyuFrame
            </h1>
            <p style={{ fontSize: 13, color: 'rgba(255,255,255,0.35)' }}>
              Choisissez l'expérience qui vous correspond
            </p>
          </div>

          {/* Current plan badge */}
          <div style={{ width: 100 }} className="flex justify-end">
            {yuyuUsername && (
              <div
                className="flex flex-col items-end gap-0.5"
              >
                <span style={{ fontSize: 10, color: 'rgba(255,255,255,0.25)', fontWeight: 500 }}>Votre plan</span>
                <PlanBadge plan={effectivePlan} />
              </div>
            )}
          </div>
        </div>

        {/* Free notice banner */}
        <div
          className="flex items-center gap-3 rounded-2xl px-5 py-3"
          style={{ background: 'rgba(75,63,207,0.1)', border: '1px solid rgba(75,63,207,0.25)' }}
        >
          <svg viewBox="0 0 24 24" fill="#818cf8" width={16} height={16}>
            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z" />
          </svg>
          <p style={{ fontSize: 12, color: 'rgba(255,255,255,0.55)', lineHeight: 1.5 }}>
            <span style={{ color: '#818cf8', fontWeight: 600 }}>Toutes les fonctionnalités sont gratuites pour l'instant.</span>{' '}
            YuyuFrame est en cours de développement — la facturation sera activée lors du lancement officiel.
          </p>
        </div>

        {/* Cards */}
        <div className="grid grid-cols-3 gap-5">
          {PLANS.map((plan) => {
            const isCurrent = plan.id === effectivePlan
            return (
              <div
                key={plan.id}
                className="relative flex flex-col rounded-2xl overflow-hidden"
                style={{
                  background: `linear-gradient(145deg, rgba(255,255,255,0.03) 0%, ${plan.glowColor} 100%)`,
                  border: `1px solid ${isCurrent ? plan.borderColor : 'rgba(255,255,255,0.07)'}`,
                  boxShadow: isCurrent ? `0 0 0 1px ${plan.borderColor}, 0 8px 40px ${plan.glowColor}` : 'none',
                  transition: 'border-color 0.2s, box-shadow 0.2s',
                }}
              >
                {/* Top accent line */}
                {plan.price && (
                  <div className="h-0.5 w-full" style={{ background: `linear-gradient(90deg, transparent, ${plan.color}, transparent)` }} />
                )}

                {/* Current badge */}
                {isCurrent && (
                  <div
                    className="absolute right-3 top-3 rounded-full px-2 py-0.5"
                    style={{ background: plan.badgeBg, fontSize: 9, fontWeight: 700, color: plan.badgeColor, letterSpacing: '0.06em' }}
                  >
                    ACTUEL
                  </div>
                )}

                <div className="flex flex-col gap-6 p-6">
                  {/* Plan name & price */}
                  <div className="flex flex-col gap-2">
                    <div className="flex items-center gap-2">
                      <PlanIcon plan={plan.id} color={plan.color} />
                      <span className="font-bold" style={{ fontSize: 16, color: plan.price ? plan.color : 'rgba(255,255,255,0.7)' }}>
                        {plan.name}
                      </span>
                    </div>

                    {plan.price ? (
                      <div className="flex items-baseline gap-1">
                        <span className="font-black" style={{ fontSize: 32, color: 'white', letterSpacing: '-0.03em' }}>
                          {plan.price}€
                        </span>
                        <span style={{ fontSize: 12, color: 'rgba(255,255,255,0.3)', fontWeight: 500 }}>/mois</span>
                        <span
                          className="ml-1 rounded-full px-1.5 py-0.5"
                          style={{ fontSize: 9, fontWeight: 700, color: '#4ade80', background: 'rgba(74,222,128,0.1)', letterSpacing: '0.05em' }}
                        >
                          GRATUIT
                        </span>
                      </div>
                    ) : (
                      <div className="flex items-baseline gap-1">
                        <span className="font-black" style={{ fontSize: 32, color: 'white', letterSpacing: '-0.03em' }}>
                          0€
                        </span>
                        <span style={{ fontSize: 12, color: 'rgba(255,255,255,0.3)', fontWeight: 500 }}>/mois</span>
                      </div>
                    )}
                  </div>

                  {/* Features */}
                  <div className="flex flex-col gap-2.5">
                    {plan.features.map((feat, i) => (
                      <div key={i} className="flex items-start gap-2.5">
                        <div className="mt-0.5 flex-shrink-0">
                          {feat.ok ? (
                            <svg viewBox="0 0 16 16" fill="none" width={14} height={14}>
                              <circle cx="8" cy="8" r="7" fill={plan.price ? plan.badgeBg : 'rgba(255,255,255,0.06)'} />
                              <path d="M4.5 8l2.5 2.5 4.5-5" stroke={plan.price ? plan.color : 'rgba(255,255,255,0.4)'} strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                            </svg>
                          ) : (
                            <svg viewBox="0 0 16 16" fill="none" width={14} height={14}>
                              <circle cx="8" cy="8" r="7" fill="rgba(255,255,255,0.03)" />
                              <path d="M5.5 10.5l5-5M10.5 10.5l-5-5" stroke="rgba(255,255,255,0.15)" strokeWidth="1.5" strokeLinecap="round" />
                            </svg>
                          )}
                        </div>
                        <span style={{ fontSize: 12, color: feat.ok ? 'rgba(255,255,255,0.65)' : 'rgba(255,255,255,0.22)', lineHeight: 1.4 }}>
                          {feat.label}
                        </span>
                      </div>
                    ))}
                  </div>

                  {/* CTA */}
                  <button
                    className="w-full rounded-xl font-semibold transition-all duration-200 active:scale-95"
                    style={{
                      height: 40,
                      fontSize: 13,
                      background: isCurrent
                        ? (plan.price ? plan.badgeBg : 'rgba(255,255,255,0.06)')
                        : (plan.price ? plan.color : 'rgba(255,255,255,0.06)'),
                      color: isCurrent
                        ? (plan.price ? plan.color : 'rgba(255,255,255,0.4)')
                        : (plan.price ? '#09090D' : 'rgba(255,255,255,0.4)'),
                      border: isCurrent
                        ? `1px solid ${plan.price ? plan.borderColor : 'rgba(255,255,255,0.1)'}`
                        : 'none',
                      cursor: isCurrent ? 'default' : 'pointer',
                      fontWeight: 700,
                    }}
                    disabled={isCurrent}
                  >
                    {isCurrent ? 'Plan actuel' : plan.price ? `Passer à ${plan.name}` : 'Plan gratuit'}
                  </button>
                </div>
              </div>
            )
          })}
        </div>

        {/* Comparison table */}
        <div
          className="rounded-2xl overflow-hidden"
          style={{ border: '1px solid rgba(255,255,255,0.07)' }}
        >
          <div className="px-6 py-4" style={{ background: 'rgba(255,255,255,0.02)', borderBottom: '1px solid rgba(255,255,255,0.06)' }}>
            <span style={{ fontSize: 13, fontWeight: 700, color: 'rgba(255,255,255,0.6)', letterSpacing: '0.05em' }}>
              COMPARAISON DES QUOTAS
            </span>
          </div>
          <div className="divide-y" style={{ borderColor: 'rgba(255,255,255,0.05)' }}>
            {[
              { label: 'Comptes Minecraft', free: '2', premium: '2', ultimate: '2' },
              { label: 'Instances locales', free: 'Illimitées', premium: 'Illimitées', ultimate: 'Illimitées' },
              { label: 'Instances synchronisées', free: '—', premium: '3 avec saves / 4 sans', ultimate: '10' },
              { label: 'Saves synchronisées (total)', free: '—', premium: '3', ultimate: '10' },
            ].map((row, i) => (
              <div key={i} className="grid grid-cols-4 px-6 py-3.5">
                <span style={{ fontSize: 12, color: 'rgba(255,255,255,0.45)', fontWeight: 500 }}>{row.label}</span>
                <span style={{ fontSize: 12, color: 'rgba(255,255,255,0.3)', textAlign: 'center' }}>{row.free}</span>
                <span style={{ fontSize: 12, color: '#818cf8', textAlign: 'center', fontWeight: 500 }}>{row.premium}</span>
                <span style={{ fontSize: 12, color: '#f59e0b', textAlign: 'center', fontWeight: 500 }}>{row.ultimate}</span>
              </div>
            ))}
          </div>
          <div className="grid grid-cols-4 px-6 py-2" style={{ background: 'rgba(255,255,255,0.02)', borderTop: '1px solid rgba(255,255,255,0.06)' }}>
            <span style={{ fontSize: 10, color: 'rgba(255,255,255,0.18)' }} />
            {['Free', 'Premium', 'Ultimate'].map((name, i) => (
              <span key={i} style={{ fontSize: 10, color: ['rgba(255,255,255,0.25)', '#818cf8', '#f59e0b'][i], textAlign: 'center', fontWeight: 700, letterSpacing: '0.06em' }}>
                {name.toUpperCase()}
              </span>
            ))}
          </div>
        </div>

        {/* Footer note */}
        <div className="text-center pb-4">
          <p style={{ fontSize: 11, color: 'rgba(255,255,255,0.2)', lineHeight: 1.6 }}>
            YuyuFrame est un projet open-source en cours de développement.<br />
            Les abonnements seront activés lors du lancement officiel. Aucune carte bancaire requise pour l'instant.
          </p>
        </div>

      </div>
    </div>
  )
}

function PlanBadge({ plan }: { plan: string }) {
  if (plan === 'ultimate') {
    return (
      <span
        className="rounded-full px-2 py-0.5"
        style={{ fontSize: 10, fontWeight: 700, color: '#f59e0b', background: 'rgba(245,158,11,0.15)', letterSpacing: '0.05em' }}
      >
        ULTIMATE
      </span>
    )
  }
  if (plan === 'premium') {
    return (
      <span
        className="rounded-full px-2 py-0.5"
        style={{ fontSize: 10, fontWeight: 700, color: '#818cf8', background: 'rgba(75,63,207,0.2)', letterSpacing: '0.05em' }}
      >
        PREMIUM
      </span>
    )
  }
  return (
    <span
      className="rounded-full px-2 py-0.5"
      style={{ fontSize: 10, fontWeight: 700, color: 'rgba(255,255,255,0.4)', background: 'rgba(255,255,255,0.07)', letterSpacing: '0.05em' }}
    >
      FREE
    </span>
  )
}

function PlanIcon({ plan, color }: { plan: string; color: string }) {
  if (plan === 'ultimate') {
    return (
      <svg viewBox="0 0 20 20" fill={color} width={16} height={16}>
        <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
      </svg>
    )
  }
  if (plan === 'premium') {
    return (
      <svg viewBox="0 0 20 20" fill={color} width={16} height={16}>
        <path d="M10 2a8 8 0 100 16A8 8 0 0010 2zm0 14a6 6 0 110-12 6 6 0 010 12zm-1-9h2v4h-2V7zm0 5h2v2h-2v-2z" />
      </svg>
    )
  }
  return (
    <svg viewBox="0 0 20 20" fill={color} width={16} height={16}>
      <path d="M10 2a8 8 0 100 16A8 8 0 0010 2zm1 11H9v-2h2v2zm0-4H9V6h2v3z" />
    </svg>
  )
}
