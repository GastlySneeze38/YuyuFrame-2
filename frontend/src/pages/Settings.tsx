import { useStore } from '@/stores/useStore'
import { Sidebar } from '@/components/Sidebar'

export default function Settings() {
  const { ram, setRam, javaPath, setJavaPath, minecraftPath, setMinecraftPath, theme, toggleTheme } =
    useStore()

  return (
    <div className="flex h-full bg-bg-primary transition-theme">
      <Sidebar />

      <div className="flex flex-1 flex-col overflow-hidden">
        {/* Header */}
        <div className="border-b border-border bg-bg-secondary px-8 py-5 transition-theme">
          <h1 className="text-xl font-black tracking-wide text-txt-primary">Paramètres</h1>
          <p className="mt-0.5 text-xs text-txt-secondary">Configuration de YuyuFrame</p>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-8">
          <div className="mx-auto max-w-xl space-y-4">
            {/* Performance */}
            <SettingCard
              title="Performance"
              icon={
                <svg viewBox="0 0 24 24" fill="currentColor" className="h-4 w-4">
                  <path d="M13.49 5.48c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm-3.6 13.9l1-4.4 2.1 2v6h2v-7.5l-2.1-2 .6-3c1.3 1.5 3.3 2.5 5.5 2.5v-2c-1.9 0-3.5-1-4.3-2.4l-1-1.6c-.4-.6-1-1-1.7-1-.3 0-.5.1-.8.1l-5.2 2.2v4.7h2v-3.4l1.8-.7-1.6 8.1-4.9-1-.4 2 7 1.4z" />
                </svg>
              }
            >
              <div className="flex flex-col gap-3">
                <div className="flex justify-between text-sm">
                  <span className="font-medium text-txt-primary">RAM allouée</span>
                  <span className="font-bold text-accent">
                    {(ram / 1024).toFixed(1)} Go ({ram} Mo)
                  </span>
                </div>
                <input
                  type="range"
                  min={1024}
                  max={16384}
                  step={512}
                  value={ram}
                  onChange={(e) => setRam(Number(e.target.value))}
                  className="w-full accent-accent"
                />
                <div className="flex justify-between text-xs text-txt-secondary/60">
                  <span>1 Go</span>
                  <span>4 Go</span>
                  <span>8 Go</span>
                  <span>12 Go</span>
                  <span>16 Go</span>
                </div>
                <p className="text-xs text-txt-secondary">
                  Recommandé : 2 Go minimum pour vanilla, 4–6 Go pour les modpacks.
                </p>
              </div>
            </SettingCard>

            {/* Chemins */}
            <SettingCard
              title="Chemins"
              icon={
                <svg viewBox="0 0 24 24" fill="currentColor" className="h-4 w-4">
                  <path d="M20 6h-8l-2-2H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm0 12H4V8h16v10z" />
                </svg>
              }
            >
              <div className="flex flex-col gap-4">
                <PathInput
                  label="Exécutable Java"
                  placeholder="Défaut : java (dans PATH)"
                  value={javaPath}
                  onChange={setJavaPath}
                />
                <PathInput
                  label="Dossier .minecraft"
                  placeholder="Défaut : %APPDATA%\YuyuFrame\.minecraft"
                  value={minecraftPath}
                  onChange={setMinecraftPath}
                />
              </div>
            </SettingCard>

            {/* Apparence */}
            <SettingCard
              title="Apparence"
              icon={
                <svg viewBox="0 0 24 24" fill="currentColor" className="h-4 w-4">
                  <path d="M12 3c-4.97 0-9 4.03-9 9s4.03 9 9 9c.83 0 1.5-.67 1.5-1.5 0-.39-.15-.74-.39-1.01-.23-.26-.38-.61-.38-.99 0-.83.67-1.5 1.5-1.5H16c2.76 0 5-2.24 5-5 0-4.42-4.03-8-9-8zm-5.5 9c-.83 0-1.5-.67-1.5-1.5S5.67 9 6.5 9 8 9.67 8 10.5 7.33 12 6.5 12zm3-4C8.67 8 8 7.33 8 6.5S8.67 5 9.5 5s1.5.67 1.5 1.5S10.33 8 9.5 8zm5 0c-.83 0-1.5-.67-1.5-1.5S13.67 5 14.5 5s1.5.67 1.5 1.5S15.33 8 14.5 8zm3 4c-.83 0-1.5-.67-1.5-1.5S16.67 9 17.5 9s1.5.67 1.5 1.5-.67 1.5-1.5 1.5z" />
                </svg>
              }
            >
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-txt-primary">Thème</p>
                  <p className="text-xs text-txt-secondary">
                    Actuellement : <span className="font-semibold text-accent capitalize">{theme}</span>
                  </p>
                </div>
                <button
                  onClick={toggleTheme}
                  className="rounded-xl border border-border bg-bg-secondary px-4 py-2 text-sm font-medium text-txt-primary transition-theme hover:border-accent hover:text-accent"
                >
                  {theme === 'chill' ? '🎮 Passer en Gamer' : '❄️ Passer en Chill'}
                </button>
              </div>
            </SettingCard>

            {/* À propos */}
            <SettingCard
              title="À propos"
              icon={
                <svg viewBox="0 0 24 24" fill="currentColor" className="h-4 w-4">
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z" />
                </svg>
              }
            >
              <div className="space-y-2 text-sm">
                <InfoRow label="Launcher" value="YuyuFrame v2.0" />
                <InfoRow label="Stack" value="Electron · React · Rust" />
                <InfoRow label="Backend" value="localhost:3847" />
                <InfoRow label="Auteur" value="Ghasty" />
              </div>
            </SettingCard>
          </div>
        </div>
      </div>
    </div>
  )
}

function SettingCard({
  title,
  icon,
  children,
}: {
  title: string
  icon: React.ReactNode
  children: React.ReactNode
}) {
  return (
    <div className="rounded-2xl border border-border bg-bg-card p-5 transition-theme">
      <div className="mb-4 flex items-center gap-2.5">
        <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-accent/15 text-accent">
          {icon}
        </div>
        <h2 className="text-sm font-bold text-txt-primary">{title}</h2>
      </div>
      {children}
    </div>
  )
}

function PathInput({
  label,
  placeholder,
  value,
  onChange,
}: {
  label: string
  placeholder: string
  value: string
  onChange: (v: string) => void
}) {
  return (
    <div>
      <label className="mb-1.5 block text-xs font-medium text-txt-secondary">{label}</label>
      <input
        type="text"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        className="w-full rounded-lg border border-border bg-bg-secondary px-3 py-2 text-sm text-txt-primary placeholder:text-txt-secondary/40 outline-none transition-theme focus:border-accent"
      />
    </div>
  )
}

function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between">
      <span className="text-txt-secondary">{label}</span>
      <span className="font-medium text-txt-primary">{value}</span>
    </div>
  )
}
