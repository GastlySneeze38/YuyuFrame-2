import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '@/api/client'
import { useStore } from '@/stores/useStore'
import type { Instance, Mod } from '@/types'

// ── Modrinth types ────────────────────────────────────────────────────────────

interface ModrinthHit {
  project_id: string
  slug: string
  title: string
  description: string
  icon_url: string | null
  downloads: number
  categories: string[]
}

interface ModrinthVersion {
  files: Array<{ url: string; filename: string; primary: boolean }>
}

// ── Helpers ───────────────────────────────────────────────────────────────────

function formatSize(bytes: number): string {
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} Ko`
  return `${(bytes / (1024 * 1024)).toFixed(1)} Mo`
}

function formatDownloads(n: number): string {
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`
  if (n >= 1_000) return `${(n / 1_000).toFixed(0)}k`
  return String(n)
}

function displayName(name: string): string {
  return name.replace(/\.jar(\.disabled)?$/, '')
}

async function fetchModrinthSearch(
  query: string,
  gameVersion: string,
  loader: string,
): Promise<ModrinthHit[]> {
  const facets: string[][] = [['project_type:mod']]
  if (gameVersion) facets.push([`versions:${gameVersion}`])
  if (loader && loader !== 'vanilla') facets.push([`categories:${loader}`])

  const params = new URLSearchParams({
    query,
    facets: JSON.stringify(facets),
    limit: '20',
  })

  const res = await fetch(`https://api.modrinth.com/v2/search?${params}`, {
    headers: { 'User-Agent': 'YuyuFrame/1.0' },
  })
  if (!res.ok) throw new Error(`Modrinth: ${res.status}`)
  const data = await res.json()
  return data.hits as ModrinthHit[]
}

async function fetchLatestVersion(
  slug: string,
  gameVersion: string,
  loader: string,
): Promise<ModrinthVersion | null> {
  const params = new URLSearchParams()
  if (gameVersion) params.set('game_versions', JSON.stringify([gameVersion]))
  if (loader && loader !== 'vanilla') params.set('loaders', JSON.stringify([loader]))

  const res = await fetch(
    `https://api.modrinth.com/v2/project/${slug}/version?${params}`,
    { headers: { 'User-Agent': 'YuyuFrame/1.0' } },
  )
  if (!res.ok) return null
  const versions: ModrinthVersion[] = await res.json()
  return versions[0] ?? null
}

type Tab = 'installed' | 'browse'

// ── ModsContent — embeddable in any page ──────────────────────────────────────

export function ModsContent({ instance }: { instance: Instance }) {
  const instanceId = instance.id
  const mcVersion = instance.mc_version
  const loader = instance.loader

  const [tab, setTab] = useState<Tab>('installed')
  const [mods, setMods] = useState<Mod[]>([])
  const [loadingMods, setLoadingMods] = useState(true)
  const [modsError, setModsError] = useState('')
  const [uploading, setUploading] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

  const [query, setQuery] = useState('')
  const [results, setResults] = useState<ModrinthHit[]>([])
  const [searching, setSearching] = useState(false)
  const [searchError, setSearchError] = useState('')
  const [installing, setInstalling] = useState<string | null>(null)
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  const loadMods = async () => {
    if (!instanceId) return
    setLoadingMods(true)
    setModsError('')
    try {
      setMods(await api.mods.list(instanceId))
    } catch {
      setModsError('Impossible de charger les mods')
    } finally {
      setLoadingMods(false)
    }
  }

  useEffect(() => { loadMods() }, [instanceId])

  useEffect(() => {
    if (tab === 'browse' && results.length === 0 && !searching) {
      runSearch(query)
    }
  }, [tab])

  const handleToggle = async (mod: Mod) => {
    try {
      const updated = await api.mods.toggle(instanceId, mod.name)
      setMods((prev) => prev.map((m) => m.name === mod.name ? updated : m))
    } catch { /* ignore */ }
  }

  const handleDelete = async (name: string) => {
    try {
      await api.mods.delete(instanceId, name)
      setMods((prev) => prev.filter((m) => m.name !== name))
    } catch { /* ignore */ }
  }

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    setUploading(true)
    try {
      const newMod = await api.mods.upload(instanceId, file)
      setMods((prev) =>
        [...prev.filter((m) => m.name !== newMod.name), newMod]
          .sort((a, b) => a.name.toLowerCase().localeCompare(b.name.toLowerCase()))
      )
    } catch {
      setModsError("Erreur lors de l'import")
    } finally {
      setUploading(false)
      if (fileInputRef.current) fileInputRef.current.value = ''
    }
  }

  const runSearch = async (q: string) => {
    setSearching(true)
    setSearchError('')
    try {
      setResults(await fetchModrinthSearch(q, mcVersion, loader))
    } catch {
      setSearchError('Impossible de joindre Modrinth')
    } finally {
      setSearching(false)
    }
  }

  const handleQueryChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const q = e.target.value
    setQuery(q)
    if (debounceRef.current) clearTimeout(debounceRef.current)
    debounceRef.current = setTimeout(() => runSearch(q), 450)
  }

  const isInstalled = (slug: string) =>
    mods.some((m) => displayName(m.name).toLowerCase().includes(slug.toLowerCase()))

  const handleInstall = async (hit: ModrinthHit) => {
    setInstalling(hit.project_id)
    try {
      const version = await fetchLatestVersion(hit.slug, mcVersion, loader)
      if (!version) throw new Error('Aucune version compatible')
      const file = version.files.find((f) => f.primary) ?? version.files[0]
      if (!file) throw new Error('Aucun fichier disponible')
      const newMod = await api.mods.install(instanceId, file.url, file.filename)
      setMods((prev) =>
        [...prev.filter((m) => m.name !== newMod.name), newMod]
          .sort((a, b) => a.name.toLowerCase().localeCompare(b.name.toLowerCase()))
      )
    } catch (e) {
      setSearchError(e instanceof Error ? e.message : 'Erreur installation')
    } finally {
      setInstalling(null)
    }
  }

  return (
    <div className="flex h-full flex-col overflow-hidden">
      {/* Sub-header: instance info + tabs + upload */}
      <div
        className="flex flex-shrink-0 items-center justify-between gap-4 px-6 py-3"
        style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}
      >
        <div className="flex gap-1">
          {(['installed', 'browse'] as Tab[]).map((t) => (
            <button
              key={t}
              onClick={() => setTab(t)}
              className="rounded-lg px-4 py-1.5 text-xs font-semibold transition-all duration-150"
              style={{
                background: tab === t ? 'rgba(75,63,207,0.25)' : 'transparent',
                color: tab === t ? 'rgba(255,255,255,0.9)' : 'rgba(255,255,255,0.35)',
                border: `1px solid ${tab === t ? 'rgba(75,63,207,0.5)' : 'transparent'}`,
              }}
            >
              {t === 'installed' ? `Installés (${mods.length})` : 'Parcourir Modrinth'}
            </button>
          ))}
        </div>

        <div className="flex items-center gap-3">
          <span style={{ fontSize: 11, color: 'rgba(255,255,255,0.25)' }}>
            {instance.name} · {mcVersion} · {loader}
          </span>
          {tab === 'installed' && (
            <>
              <button
                onClick={() => fileInputRef.current?.click()}
                disabled={uploading}
                className="flex items-center gap-2 font-semibold transition-all duration-200 active:scale-95"
                style={{
                  height: 32, paddingLeft: 12, paddingRight: 12, borderRadius: 10, fontSize: 12,
                  background: uploading ? 'rgba(40,38,65,0.7)' : '#4B3FCF',
                  cursor: uploading ? 'not-allowed' : 'pointer',
                }}
                onMouseEnter={(e) => { if (!uploading) e.currentTarget.style.background = '#6155e8' }}
                onMouseLeave={(e) => { if (!uploading) e.currentTarget.style.background = '#4B3FCF' }}
              >
                <svg viewBox="0 0 24 24" fill="currentColor" width={13} height={13}>
                  <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z" />
                </svg>
                {uploading ? 'Import...' : 'Importer'}
              </button>
              <input ref={fileInputRef} type="file" accept=".jar" className="hidden" onChange={handleFileChange} />
            </>
          )}
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto px-6 py-3">
        {tab === 'installed' ? (
          <InstalledTab
            mods={mods}
            loading={loadingMods}
            error={modsError}
            onReload={loadMods}
            onToggle={handleToggle}
            onDelete={handleDelete}
          />
        ) : (
          <BrowseTab
            query={query}
            results={results}
            searching={searching}
            error={searchError}
            installing={installing}
            isInstalled={isInstalled}
            onQueryChange={handleQueryChange}
            onInstall={handleInstall}
          />
        )}
      </div>
    </div>
  )
}

// ── Standalone page (kept for /mods route) ────────────────────────────────────

export default function Mods() {
  const navigate = useNavigate()
  const { selectedInstance } = useStore()
  const instance = selectedInstance()

  if (!instance) {
    return (
      <div className="flex h-full flex-col items-center justify-center gap-4" style={{ background: '#09090D', color: 'white' }}>
        <div style={{ fontSize: 36 }}>🧱</div>
        <p style={{ fontSize: 14, color: 'rgba(255,255,255,0.4)', fontWeight: 600 }}>Aucune instance sélectionnée</p>
        <button
          onClick={() => navigate('/instances')}
          className="font-semibold transition-all duration-200 active:scale-95"
          style={{ height: 38, padding: '0 20px', borderRadius: 10, fontSize: 13, background: '#4B3FCF', color: 'white' }}
        >
          Gérer les instances
        </button>
      </div>
    )
  }

  return (
    <div className="flex h-full flex-col" style={{ background: '#09090D', color: 'white' }}>
      <div
        className="flex flex-shrink-0 items-center gap-3 px-6 py-3"
        style={{ borderBottom: '1px solid rgba(255,255,255,0.06)' }}
      >
        <button
          onClick={() => navigate('/home')}
          className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-lg transition-all duration-150"
          style={{ color: 'rgba(255,255,255,0.35)', background: 'rgba(255,255,255,0.04)' }}
          onMouseEnter={(e) => { e.currentTarget.style.color = 'rgba(255,255,255,0.7)'; e.currentTarget.style.background = 'rgba(255,255,255,0.08)' }}
          onMouseLeave={(e) => { e.currentTarget.style.color = 'rgba(255,255,255,0.35)'; e.currentTarget.style.background = 'rgba(255,255,255,0.04)' }}
        >
          <svg viewBox="0 0 24 24" fill="currentColor" style={{ width: 15, height: 15 }}>
            <path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z" />
          </svg>
        </button>
        <h1 className="font-black text-white" style={{ fontSize: 18, letterSpacing: '-0.01em' }}>Mods</h1>
      </div>
      <ModsContent instance={instance} />
    </div>
  )
}

// ── Installed tab ─────────────────────────────────────────────────────────────

function InstalledTab({
  mods, loading, error, onReload, onToggle, onDelete,
}: {
  mods: Mod[]
  loading: boolean
  error: string
  onReload: () => void
  onToggle: (mod: Mod) => void
  onDelete: (name: string) => void
}) {
  if (loading) return <Spinner />
  if (error) return <ErrorState message={error} onRetry={onReload} />
  if (mods.length === 0) return (
    <EmptyState
      icon={<PlugIcon size={28} color="rgba(75,63,207,0.55)" />}
      title="Aucun mod installé"
      subtitle={'Cliquez sur "Importer" ou parcourez Modrinth'}
    />
  )
  return (
    <div className="flex flex-col gap-2">
      {mods.map((mod) => (
        <ModRow key={mod.name} mod={mod} onToggle={() => onToggle(mod)} onDelete={() => onDelete(mod.name)} />
      ))}
    </div>
  )
}

// ── Browse tab ────────────────────────────────────────────────────────────────

function BrowseTab({
  query, results, searching, error, installing, isInstalled,
  onQueryChange, onInstall,
}: {
  query: string
  results: ModrinthHit[]
  searching: boolean
  error: string
  installing: string | null
  isInstalled: (slug: string) => boolean
  onQueryChange: (e: React.ChangeEvent<HTMLInputElement>) => void
  onInstall: (hit: ModrinthHit) => void
}) {
  return (
    <div className="flex flex-col gap-3">
      <div className="relative">
        <svg viewBox="0 0 24 24" fill="currentColor" width={15} height={15}
          className="absolute left-3 top-1/2 -translate-y-1/2"
          style={{ color: 'rgba(255,255,255,0.3)', pointerEvents: 'none' }}>
          <path d="M15.5 14h-.79l-.28-.27A6.471 6.471 0 0016 9.5 6.5 6.5 0 109.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z" />
        </svg>
        <input
          type="text"
          placeholder="Rechercher un mod..."
          value={query}
          onChange={onQueryChange}
          className="w-full rounded-xl pl-9 pr-4 text-sm text-white outline-none"
          style={{ height: 40, background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.08)' }}
          onFocus={(e) => { e.currentTarget.style.borderColor = 'rgba(75,63,207,0.6)' }}
          onBlur={(e) => { e.currentTarget.style.borderColor = 'rgba(255,255,255,0.08)' }}
        />
        {searching && (
          <span className="absolute right-3 top-1/2 -translate-y-1/2 h-4 w-4 animate-spin rounded-full border-2"
            style={{ borderColor: 'rgba(255,255,255,0.1)', borderTopColor: 'rgba(75,63,207,0.8)' }} />
        )}
      </div>

      {error && <p style={{ fontSize: 12, color: 'rgb(248,113,113)' }}>{error}</p>}

      {!searching && results.length === 0 && !error && (
        <EmptyState
          icon={<SearchIcon size={28} color="rgba(255,255,255,0.15)" />}
          title="Aucun résultat"
          subtitle="Essayez un autre terme ou changez la version MC"
        />
      )}

      <div className="flex flex-col gap-2">
        {results.map((hit) => (
          <ModrinthCard
            key={hit.project_id}
            hit={hit}
            installed={isInstalled(hit.slug)}
            loading={installing === hit.project_id}
            onInstall={() => onInstall(hit)}
          />
        ))}
      </div>
    </div>
  )
}

// ── Mod row ───────────────────────────────────────────────────────────────────

function ModRow({ mod, onToggle, onDelete }: { mod: Mod; onToggle: () => void; onDelete: () => void }) {
  const [confirm, setConfirm] = useState(false)
  return (
    <div
      className="flex items-center gap-3 rounded-2xl px-4 py-3 transition-all duration-150"
      style={{ background: mod.enabled ? 'rgba(255,255,255,0.04)' : 'rgba(255,255,255,0.018)', border: '1px solid rgba(255,255,255,0.06)', opacity: mod.enabled ? 1 : 0.6 }}
    >
      <div style={{ width: 36, height: 36, borderRadius: 10, flexShrink: 0, background: mod.enabled ? 'rgba(75,63,207,0.15)' : 'rgba(255,255,255,0.05)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <PlugIcon size={18} color={mod.enabled ? 'rgba(120,110,230,0.8)' : 'rgba(255,255,255,0.2)'} />
      </div>
      <div className="min-w-0 flex-1">
        <p className="truncate font-semibold" style={{ fontSize: 13, color: mod.enabled ? 'rgba(255,255,255,0.9)' : 'rgba(255,255,255,0.4)' }}>
          {displayName(mod.name)}
        </p>
        <p style={{ fontSize: 11, color: 'rgba(255,255,255,0.22)', marginTop: 1 }}>{formatSize(mod.size)}</p>
      </div>
      <button onClick={onToggle} title={mod.enabled ? 'Désactiver' : 'Activer'}
        className="relative flex-shrink-0"
        style={{ width: 40, height: 22, borderRadius: 11, background: mod.enabled ? '#4B3FCF' : 'rgba(255,255,255,0.1)', border: 'none', cursor: 'pointer' }}>
        <span className="absolute transition-all duration-200" style={{ top: 3, left: mod.enabled ? 21 : 3, width: 16, height: 16, borderRadius: '50%', background: 'white', boxShadow: '0 1px 4px rgba(0,0,0,0.4)' }} />
      </button>
      {confirm ? (
        <div className="flex flex-shrink-0 items-center gap-1">
          <button onClick={() => { onDelete(); setConfirm(false) }} style={{ fontSize: 11, fontWeight: 600, color: 'rgb(248,113,113)', background: 'rgba(200,50,50,0.15)', borderRadius: 8, padding: '4px 10px' }}>Supprimer</button>
          <button onClick={() => setConfirm(false)} style={{ fontSize: 11, color: 'rgba(255,255,255,0.4)', background: 'rgba(255,255,255,0.06)', borderRadius: 8, padding: '4px 10px' }}>Annuler</button>
        </div>
      ) : (
        <button onClick={() => setConfirm(true)}
          className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-xl transition-all duration-150"
          style={{ color: 'rgba(255,255,255,0.2)' }}
          onMouseEnter={(e) => { e.currentTarget.style.color = 'rgb(248,113,113)'; e.currentTarget.style.background = 'rgba(200,50,50,0.12)' }}
          onMouseLeave={(e) => { e.currentTarget.style.color = 'rgba(255,255,255,0.2)'; e.currentTarget.style.background = 'transparent' }}>
          <svg viewBox="0 0 24 24" fill="currentColor" width={16} height={16}>
            <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z" />
          </svg>
        </button>
      )}
    </div>
  )
}

// ── Modrinth card ─────────────────────────────────────────────────────────────

function ModrinthCard({ hit, installed, loading, onInstall }: {
  hit: ModrinthHit; installed: boolean; loading: boolean; onInstall: () => void
}) {
  return (
    <div className="flex items-center gap-3 rounded-2xl px-4 py-3" style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.06)' }}>
      <div style={{ width: 44, height: 44, borderRadius: 12, flexShrink: 0, overflow: 'hidden', background: 'rgba(255,255,255,0.06)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        {hit.icon_url ? (
          <img src={hit.icon_url} alt={hit.title} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
        ) : (
          <PlugIcon size={20} color="rgba(255,255,255,0.2)" />
        )}
      </div>
      <div className="min-w-0 flex-1">
        <p className="truncate font-semibold text-white" style={{ fontSize: 13 }}>{hit.title}</p>
        <p className="truncate" style={{ fontSize: 11, color: 'rgba(255,255,255,0.35)', marginTop: 2 }}>{hit.description}</p>
        <p style={{ fontSize: 10, color: 'rgba(255,255,255,0.2)', marginTop: 3 }}>{formatDownloads(hit.downloads)} téléchargements</p>
      </div>
      <button
        onClick={onInstall}
        disabled={installed || loading}
        className="flex-shrink-0 flex items-center gap-1.5 rounded-xl font-semibold transition-all duration-150 active:scale-95"
        style={{
          height: 32, paddingLeft: 14, paddingRight: 14, fontSize: 12,
          background: installed ? 'rgba(255,255,255,0.05)' : loading ? 'rgba(40,38,65,0.7)' : 'rgba(75,63,207,0.3)',
          border: `1px solid ${installed ? 'rgba(255,255,255,0.08)' : 'rgba(75,63,207,0.5)'}`,
          color: installed ? 'rgba(255,255,255,0.3)' : 'rgba(255,255,255,0.85)',
          cursor: installed || loading ? 'not-allowed' : 'pointer',
        }}
        onMouseEnter={(e) => { if (!installed && !loading) e.currentTarget.style.background = 'rgba(75,63,207,0.5)' }}
        onMouseLeave={(e) => { if (!installed && !loading) e.currentTarget.style.background = 'rgba(75,63,207,0.3)' }}
      >
        {loading ? (
          <span className="h-3 w-3 animate-spin rounded-full border-2" style={{ borderColor: 'rgba(255,255,255,0.15)', borderTopColor: 'white' }} />
        ) : installed ? '✓ Installé' : 'Installer'}
      </button>
    </div>
  )
}

// ── Shared small components ───────────────────────────────────────────────────

function Spinner() {
  return (
    <div className="flex h-40 items-center justify-center">
      <span className="h-8 w-8 animate-spin rounded-full border-2" style={{ borderColor: 'rgba(255,255,255,0.08)', borderTopColor: 'rgba(75,63,207,0.8)' }} />
    </div>
  )
}

function ErrorState({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <div className="flex h-40 flex-col items-center justify-center gap-3">
      <span style={{ color: 'rgba(255,255,255,0.3)', fontSize: 13 }}>{message}</span>
      <button onClick={onRetry} style={{ fontSize: 12, color: '#7872e8', textDecoration: 'underline' }}>Réessayer</button>
    </div>
  )
}

function EmptyState({ icon, title, subtitle }: { icon: React.ReactNode; title: string; subtitle: string }) {
  return (
    <div className="flex h-48 flex-col items-center justify-center gap-4">
      <div style={{ width: 56, height: 56, borderRadius: 16, background: 'rgba(255,255,255,0.04)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        {icon}
      </div>
      <div className="text-center">
        <p className="font-semibold" style={{ color: 'rgba(255,255,255,0.5)', fontSize: 14 }}>{title}</p>
        <p style={{ color: 'rgba(255,255,255,0.2)', fontSize: 12, marginTop: 4 }}>{subtitle}</p>
      </div>
    </div>
  )
}

function PlugIcon({ size, color }: { size: number; color: string }) {
  return (
    <svg viewBox="0 0 24 24" fill={color} width={size} height={size}>
      <path d="M20.5 11H19V7c0-1.1-.9-2-2-2h-4V3.5C13 2.12 11.88 1 10.5 1S8 2.12 8 3.5V5H4c-1.1 0-1.99.9-1.99 2v3.8H3.5c1.49 0 2.7 1.21 2.7 2.7s-1.21 2.7-2.7 2.7H2V20c0 1.1.9 2 2 2h3.8v-1.5c0-1.49 1.21-2.7 2.7-2.7 1.49 0 2.7 1.21 2.7 2.7V22H17c1.1 0 2-.9 2-2v-4h1.5c1.38 0 2.5-1.12 2.5-2.5S21.88 11 20.5 11z" />
    </svg>
  )
}

function SearchIcon({ size, color }: { size: number; color: string }) {
  return (
    <svg viewBox="0 0 24 24" fill={color} width={size} height={size}>
      <path d="M15.5 14h-.79l-.28-.27A6.471 6.471 0 0016 9.5 6.5 6.5 0 109.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z" />
    </svg>
  )
}
