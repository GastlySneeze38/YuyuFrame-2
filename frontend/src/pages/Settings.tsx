import { useNavigate } from 'react-router-dom'
import { useStore } from '@/stores/useStore'

export default function Settings() {
  const navigate = useNavigate()
  const { ram, setRam, javaPath, setJavaPath, minecraftPath, setMinecraftPath, theme, toggleTheme, brightness, setBrightness } = useStore()

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
            Paramètres
          </h1>
          <p style={{ fontSize: 10, color: 'rgba(255,255,255,0.28)', marginTop: 1 }}>
            Configuration de YuyuFrame
          </p>
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto p-8">
        <div className="mx-auto flex max-w-2xl flex-col gap-4">

          {/* Performance */}
          <SCard
            title="Performance"
            icon={
              <svg viewBox="0 0 24 24" fill="currentColor" className="h-4 w-4">
                <path d="M13.49 5.48c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm-3.6 13.9l1-4.4 2.1 2v6h2v-7.5l-2.1-2 .6-3c1.3 1.5 3.3 2.5 5.5 2.5v-2c-1.9 0-3.5-1-4.3-2.4l-1-1.6c-.4-.6-1-1-1.7-1-.3 0-.5.1-.8.1l-5.2 2.2v4.7h2v-3.4l1.8-.7-1.6 8.1-4.9-1-.4 2 7 1.4z" />
              </svg>
            }
          >
            <div className="flex flex-col gap-4">
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium text-white">RAM allouée</span>
                <span className="text-sm font-bold" style={{ color: '#7b72e9' }}>
                  {(ram / 1024).toFixed(1)} Go
                </span>
              </div>
              <input
                type="range"
                min={1024} max={16384} step={512}
                value={ram}
                onChange={(e) => setRam(Number(e.target.value))}
                className="w-full"
                style={{ accentColor: '#4B3FCF' }}
              />
              <div className="flex justify-between" style={{ fontSize: 10, color: 'rgba(255,255,255,0.25)' }}>
                <span>1 Go</span><span>4 Go</span><span>8 Go</span><span>12 Go</span><span>16 Go</span>
              </div>
              <p style={{ fontSize: 11, color: 'rgba(255,255,255,0.3)' }}>
                Recommandé : 2 Go minimum pour vanilla, 4–6 Go pour les modpacks.
              </p>
            </div>
          </SCard>

          {/* Chemins */}
          <SCard
            title="Chemins"
            icon={
              <svg viewBox="0 0 24 24" fill="currentColor" className="h-4 w-4">
                <path d="M20 6h-8l-2-2H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm0 12H4V8h16v10z" />
              </svg>
            }
          >
            <div className="flex flex-col gap-5">
              <PInput
                label="Exécutable Java"
                placeholder="Défaut : java (dans PATH)"
                value={javaPath}
                onChange={setJavaPath}
              />
              <PInput
                label="Dossier .minecraft"
                placeholder="Défaut : %APPDATA%\YuyuFrame\.minecraft"
                value={minecraftPath}
                onChange={setMinecraftPath}
              />
            </div>
          </SCard>

          {/* Apparence */}
          <SCard
            title="Apparence"
            icon={
              <svg viewBox="0 0 24 24" fill="currentColor" className="h-4 w-4">
                <path d="M12 3c-4.97 0-9 4.03-9 9s4.03 9 9 9c.83 0 1.5-.67 1.5-1.5 0-.39-.15-.74-.39-1.01-.23-.26-.38-.61-.38-.99 0-.83.67-1.5 1.5-1.5H16c2.76 0 5-2.24 5-5 0-4.42-4.03-8-9-8zm-5.5 9c-.83 0-1.5-.67-1.5-1.5S5.67 9 6.5 9 8 9.67 8 10.5 7.33 12 6.5 12zm3-4C8.67 8 8 7.33 8 6.5S8.67 5 9.5 5s1.5.67 1.5 1.5S10.33 8 9.5 8zm5 0c-.83 0-1.5-.67-1.5-1.5S13.67 5 14.5 5s1.5.67 1.5 1.5S15.33 8 14.5 8zm3 4c-.83 0-1.5-.67-1.5-1.5S16.67 9 17.5 9s1.5.67 1.5 1.5-.67 1.5-1.5 1.5z" />
              </svg>
            }
          >
            <div className="flex flex-col gap-6">
              {/* Thème */}
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-white">Thème</p>
                  <p style={{ fontSize: 11, color: 'rgba(255,255,255,0.35)', marginTop: 2 }}>
                    Actuellement :{' '}
                    <span style={{ color: '#7b72e9', fontWeight: 600 }}>{theme}</span>
                  </p>
                </div>
                <button
                  onClick={toggleTheme}
                  className="rounded-xl px-4 py-2 text-sm font-medium text-white transition-all duration-150"
                  style={{
                    background: 'rgba(75,63,207,0.15)',
                    border: '1px solid rgba(75,63,207,0.3)',
                  }}
                  onMouseEnter={(e) => {
                    e.currentTarget.style.background = 'rgba(75,63,207,0.28)'
                    e.currentTarget.style.borderColor = 'rgba(75,63,207,0.55)'
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.background = 'rgba(75,63,207,0.15)'
                    e.currentTarget.style.borderColor = 'rgba(75,63,207,0.3)'
                  }}
                >
                  {theme === 'chill' ? '🎮 Passer en Gamer' : '❄️ Passer en Chill'}
                </button>
              </div>

              {/* Séparateur */}
              <div style={{ height: 1, background: 'rgba(255,255,255,0.06)' }} />

              {/* Luminosité */}
              <div className="flex flex-col gap-4">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-white">Luminosité</p>
                    <p style={{ fontSize: 11, color: 'rgba(255,255,255,0.35)', marginTop: 2 }}>
                      Ajuste la luminosité de l'interface
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-bold" style={{ color: '#7b72e9' }}>
                      {brightness}%
                    </span>
                    {brightness !== 100 && (
                      <button
                        onClick={() => setBrightness(100)}
                        className="rounded-lg px-2 py-0.5 text-xs transition-all duration-150"
                        style={{ color: 'rgba(255,255,255,0.3)', background: 'rgba(255,255,255,0.05)' }}
                        onMouseEnter={(e) => { e.currentTarget.style.color = 'rgba(255,255,255,0.6)' }}
                        onMouseLeave={(e) => { e.currentTarget.style.color = 'rgba(255,255,255,0.3)' }}
                      >
                        Reset
                      </button>
                    )}
                  </div>
                </div>
                <input
                  type="range"
                  min={40} max={160} step={5}
                  value={brightness}
                  onChange={(e) => setBrightness(Number(e.target.value))}
                  className="w-full"
                  style={{ accentColor: '#4B3FCF' }}
                />
                <div className="flex justify-between" style={{ fontSize: 10, color: 'rgba(255,255,255,0.25)' }}>
                  <span>Sombre</span>
                  <span>Normal</span>
                  <span>Clair</span>
                </div>
              </div>
            </div>
          </SCard>

          {/* À propos */}
          <SCard
            title="À propos"
            icon={
              <svg viewBox="0 0 24 24" fill="currentColor" className="h-4 w-4">
                <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z" />
              </svg>
            }
          >
            <div className="flex flex-col gap-3">
              <IRow label="Launcher" value="YuyuFrame v2.0" />
              <IRow label="Stack" value="Electron · React · Rust" />
              <IRow label="Backend" value="localhost:3847" />
              <IRow label="Auteur" value="Ghasty" />
            </div>
          </SCard>

        </div>
      </div>
    </div>
  )
}

function SCard({ title, icon, children }: { title: string; icon: React.ReactNode; children: React.ReactNode }) {
  return (
    <div
      className="rounded-2xl p-6"
      style={{
        background: 'rgba(255,255,255,0.025)',
        border: '1px solid rgba(255,255,255,0.07)',
      }}
    >
      <div className="mb-5 flex items-center gap-3">
        <div
          className="flex h-8 w-8 items-center justify-center rounded-lg"
          style={{ background: 'rgba(75,63,207,0.2)', color: '#7b72e9' }}
        >
          {icon}
        </div>
        <h2 className="font-bold text-white" style={{ fontSize: 14, letterSpacing: '0.02em' }}>
          {title}
        </h2>
      </div>
      {children}
    </div>
  )
}

function PInput({ label, placeholder, value, onChange }: {
  label: string
  placeholder: string
  value: string
  onChange: (v: string) => void
}) {
  return (
    <div>
      <label className="mb-2 block" style={{ fontSize: 11, color: 'rgba(255,255,255,0.4)', fontWeight: 600, letterSpacing: '0.08em', textTransform: 'uppercase' }}>
        {label}
      </label>
      <input
        type="text"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        className="w-full rounded-xl px-4 py-3 text-sm text-white outline-none transition-all duration-150"
        style={{
          background: 'rgba(0,0,0,0.4)',
          border: '1px solid rgba(255,255,255,0.08)',
          '::placeholder': { color: 'rgba(255,255,255,0.2)' },
        } as React.CSSProperties}
        onFocus={(e) => { e.currentTarget.style.borderColor = 'rgba(75,63,207,0.5)' }}
        onBlur={(e) => { e.currentTarget.style.borderColor = 'rgba(255,255,255,0.08)' }}
      />
    </div>
  )
}

function IRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between">
      <span style={{ fontSize: 13, color: 'rgba(255,255,255,0.35)' }}>{label}</span>
      <span className="font-medium text-white" style={{ fontSize: 13 }}>{value}</span>
    </div>
  )
}
