import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '@/api/client'
import { useStore } from '@/stores/useStore'
import type { Instance, Loader } from '@/types'

const LOADERS: Loader[] = ['vanilla', 'fabric', 'forge']

const RAM_OPTIONS = [1024, 2048, 4096, 6144, 8192]

function formatRam(mb: number) {
  return mb >= 1024 ? `${mb / 1024} Go` : `${mb} Mo`
}

function loaderColor(loader: string) {
  if (loader === 'fabric') return '#b5a0ff'
  if (loader === 'forge') return '#f0a040'
  return 'rgba(255,255,255,0.4)'
}

// ── Create form ───────────────────────────────────────────────────────────────

function CreateForm({
  versions,
  defaultRam,
  onCreate,
}: {
  versions: string[]
  defaultRam: number
  onCreate: (instance: Instance) => void
}) {
  const [name, setName] = useState('')
  const [mcVersion, setMcVersion] = useState(versions[0] ?? '')
  const [loader, setLoader] = useState<Loader>('vanilla')
  const [ram, setRam] = useState(defaultRam)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (versions.length > 0 && !mcVersion) setMcVersion(versions[0])
  }, [versions])

  const handleCreate = async () => {
    if (!name.trim()) { setError('Nom requis'); return }
    if (!mcVersion) { setError('Sélectionne une version'); return }
    setLoading(true)
    setError('')
    try {
      const instance = await api.instances.create(name.trim(), mcVersion, loader, ram)
      setName('')
      setLoader('vanilla')
      setRam(defaultRam)
      onCreate(instance)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erreur')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div
      className="rounded-2xl p-5 flex flex-col gap-4"
      style={{ background: 'rgba(75,63,207,0.08)', border: '1px solid rgba(75,63,207,0.25)' }}
    >
      <p className="font-bold text-white" style={{ fontSize: 13 }}>Nouvelle instance</p>

      {/* Name */}
      <input
        type="text"
        placeholder="Nom de l'instance..."
        value={name}
        onChange={(e) => setName(e.target.value)}
        onKeyDown={(e) => e.key === 'Enter' && handleCreate()}
        className="w-full rounded-xl px-3 text-sm text-white outline-none"
        style={{
          height: 40,
          background: 'rgba(0,0,0,0.4)',
          border: '1px solid rgba(255,255,255,0.1)',
        }}
        onFocus={(e) => { e.currentTarget.style.borderColor = 'rgba(75,63,207,0.6)' }}
        onBlur={(e) => { e.currentTarget.style.borderColor = 'rgba(255,255,255,0.1)' }}
      />

      <div className="flex gap-3">
        {/* Version */}
        <div className="flex-1">
          <label style={{ fontSize: 10, color: 'rgba(255,255,255,0.4)', letterSpacing: '0.1em', textTransform: 'uppercase', fontWeight: 600 }}>Version MC</label>
          <div className="relative mt-1">
            <select
              value={mcVersion}
              onChange={(e) => setMcVersion(e.target.value)}
              className="w-full appearance-none rounded-xl px-3 pr-7 text-sm font-medium text-white outline-none"
              style={{ height: 40, background: 'rgba(0,0,0,0.4)', border: '1px solid rgba(255,255,255,0.1)' }}
            >
              {versions.map((v) => (
                <option key={v} value={v} style={{ background: '#111118' }}>{v}</option>
              ))}
            </select>
            <div className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2">
              <svg viewBox="0 0 10 6" fill="white" width={10} height={6} style={{ opacity: 0.4 }}>
                <path d="M0 0l5 6 5-6z" />
              </svg>
            </div>
          </div>
        </div>

        {/* Loader */}
        <div>
          <label style={{ fontSize: 10, color: 'rgba(255,255,255,0.4)', letterSpacing: '0.1em', textTransform: 'uppercase', fontWeight: 600 }}>Loader</label>
          <div className="flex gap-1 mt-1">
            {LOADERS.map((l) => (
              <button
                key={l}
                onClick={() => setLoader(l)}
                className="rounded-xl text-xs font-semibold transition-all duration-150"
                style={{
                  height: 40, padding: '0 12px',
                  background: loader === l ? 'rgba(75,63,207,0.35)' : 'rgba(0,0,0,0.35)',
                  border: `1px solid ${loader === l ? 'rgba(75,63,207,0.7)' : 'rgba(255,255,255,0.08)'}`,
                  color: loader === l ? 'rgba(255,255,255,0.95)' : 'rgba(255,255,255,0.35)',
                }}
              >
                {l.charAt(0).toUpperCase() + l.slice(1)}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* RAM */}
      <div>
        <label style={{ fontSize: 10, color: 'rgba(255,255,255,0.4)', letterSpacing: '0.1em', textTransform: 'uppercase', fontWeight: 600 }}>RAM</label>
        <div className="flex gap-1.5 mt-1">
          {RAM_OPTIONS.map((r) => (
            <button
              key={r}
              onClick={() => setRam(r)}
              className="rounded-xl text-xs font-semibold transition-all duration-150"
              style={{
                height: 34, padding: '0 10px',
                background: ram === r ? 'rgba(75,63,207,0.35)' : 'rgba(0,0,0,0.35)',
                border: `1px solid ${ram === r ? 'rgba(75,63,207,0.7)' : 'rgba(255,255,255,0.08)'}`,
                color: ram === r ? 'rgba(255,255,255,0.95)' : 'rgba(255,255,255,0.35)',
              }}
            >
              {formatRam(r)}
            </button>
          ))}
        </div>
      </div>

      {error && <p style={{ fontSize: 12, color: 'rgb(248,113,113)' }}>{error}</p>}

      <button
        onClick={handleCreate}
        disabled={loading}
        className="w-full font-bold text-white transition-all duration-200 active:scale-95"
        style={{
          height: 42, borderRadius: 12, fontSize: 13,
          background: loading ? 'rgba(40,38,65,0.7)' : '#4B3FCF',
          boxShadow: loading ? 'none' : '0 4px 20px rgba(75,63,207,0.35)',
          cursor: loading ? 'not-allowed' : 'pointer',
        }}
        onMouseEnter={(e) => { if (!loading) e.currentTarget.style.background = '#6155e8' }}
        onMouseLeave={(e) => { if (!loading) e.currentTarget.style.background = '#4B3FCF' }}
      >
        {loading ? 'Création...' : 'Créer l\'instance'}
      </button>
    </div>
  )
}

// ── Instance card ─────────────────────────────────────────────────────────────

function InstanceCard({
  instance,
  selected,
  onSelect,
  onDelete,
}: {
  instance: Instance
  selected: boolean
  onSelect: () => void
  onDelete: () => void
}) {
  const [confirm, setConfirm] = useState(false)

  return (
    <div
      onClick={onSelect}
      className="flex items-center gap-3 rounded-2xl px-4 py-3 cursor-pointer transition-all duration-150"
      style={{
        background: selected ? 'rgba(75,63,207,0.18)' : 'rgba(255,255,255,0.04)',
        border: `1px solid ${selected ? 'rgba(75,63,207,0.55)' : 'rgba(255,255,255,0.06)'}`,
        boxShadow: selected ? '0 0 20px rgba(75,63,207,0.18)' : 'none',
      }}
      onMouseEnter={(e) => {
        if (!selected) e.currentTarget.style.background = 'rgba(255,255,255,0.06)'
      }}
      onMouseLeave={(e) => {
        if (!selected) e.currentTarget.style.background = 'rgba(255,255,255,0.04)'
      }}
    >
      {/* Icon */}
      <div
        className="flex items-center justify-center rounded-xl flex-shrink-0"
        style={{
          width: 42, height: 42,
          background: selected ? 'rgba(75,63,207,0.3)' : 'rgba(255,255,255,0.05)',
          fontSize: 18,
        }}
      >
        🧱
      </div>

      {/* Info */}
      <div className="min-w-0 flex-1">
        <p className="font-bold truncate" style={{ fontSize: 13, color: selected ? 'white' : 'rgba(255,255,255,0.85)' }}>
          {instance.name}
        </p>
        <div className="flex items-center gap-2 mt-0.5">
          <span style={{ fontSize: 11, color: 'rgba(255,255,255,0.3)' }}>{instance.mc_version}</span>
          <span style={{ fontSize: 10, color: loaderColor(instance.loader), fontWeight: 600 }}>
            {instance.loader}
          </span>
          <span style={{ fontSize: 10, color: 'rgba(255,255,255,0.2)' }}>{formatRam(instance.ram_mb)}</span>
        </div>
      </div>

      {/* Delete */}
      <div onClick={(e) => e.stopPropagation()}>
        {confirm ? (
          <div className="flex items-center gap-1">
            <button
              onClick={() => onDelete()}
              style={{ fontSize: 11, fontWeight: 600, color: 'rgb(248,113,113)', background: 'rgba(200,50,50,0.15)', borderRadius: 8, padding: '4px 10px' }}
            >
              Supprimer
            </button>
            <button
              onClick={() => setConfirm(false)}
              style={{ fontSize: 11, color: 'rgba(255,255,255,0.4)', background: 'rgba(255,255,255,0.06)', borderRadius: 8, padding: '4px 10px' }}
            >
              Annuler
            </button>
          </div>
        ) : (
          <button
            onClick={() => setConfirm(true)}
            className="flex h-8 w-8 items-center justify-center rounded-xl transition-all duration-150"
            style={{ color: 'rgba(255,255,255,0.2)' }}
            onMouseEnter={(e) => { e.currentTarget.style.color = 'rgb(248,113,113)'; e.currentTarget.style.background = 'rgba(200,50,50,0.12)' }}
            onMouseLeave={(e) => { e.currentTarget.style.color = 'rgba(255,255,255,0.2)'; e.currentTarget.style.background = 'transparent' }}
          >
            <svg viewBox="0 0 24 24" fill="currentColor" width={16} height={16}>
              <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z" />
            </svg>
          </button>
        )}
      </div>
    </div>
  )
}

// ── Page ──────────────────────────────────────────────────────────────────────

export default function Instances() {
  const navigate = useNavigate()
  const {
    versions, setVersions,
    instances, setInstances, addInstance, removeInstance,
    selectedInstanceId, setSelectedInstanceId,
    ram,
  } = useStore()

  const [loading, setLoading] = useState(true)
  const loaded = useRef(false)

  useEffect(() => {
    if (loaded.current) return
    loaded.current = true

    Promise.all([
      api.instances.list(),
      versions.length === 0 ? api.versions.list() : Promise.resolve(null),
    ]).then(([insts, vers]) => {
      setInstances(insts)
      if (vers) setVersions(vers)
    }).finally(() => setLoading(false))
  }, [])

  const releaseVersions = versions
    .filter((v) => v.version_type === 'release')
    .map((v) => v.id)

  const handleDelete = async (id: string) => {
    try {
      await api.instances.delete(id)
      removeInstance(id)
    } catch { /* ignore */ }
  }

  return (
    <div className="flex h-full flex-col" style={{ background: '#09090D', color: 'white' }}>

      {/* Header */}
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
        <div>
          <h1 className="font-black text-white" style={{ fontSize: 18, letterSpacing: '-0.01em' }}>Instances</h1>
          <p style={{ fontSize: 11, color: 'rgba(255,255,255,0.3)', marginTop: 1 }}>
            {instances.length} instance{instances.length !== 1 ? 's' : ''}
          </p>
        </div>
      </div>

      <div className="flex flex-1 gap-5 overflow-hidden p-5">

        {/* Left: instance list */}
        <div className="flex flex-1 flex-col gap-2 overflow-y-auto">
          {loading ? (
            <div className="flex h-40 items-center justify-center">
              <span className="h-8 w-8 animate-spin rounded-full border-2" style={{ borderColor: 'rgba(255,255,255,0.08)', borderTopColor: 'rgba(75,63,207,0.8)' }} />
            </div>
          ) : instances.length === 0 ? (
            <div className="flex h-48 flex-col items-center justify-center gap-3">
              <div style={{ fontSize: 36 }}>🧱</div>
              <p style={{ fontSize: 14, color: 'rgba(255,255,255,0.4)', fontWeight: 600 }}>Aucune instance</p>
              <p style={{ fontSize: 12, color: 'rgba(255,255,255,0.2)' }}>Crée ta première instance à droite</p>
            </div>
          ) : (
            instances.map((inst) => (
              <InstanceCard
                key={inst.id}
                instance={inst}
                selected={inst.id === selectedInstanceId}
                onSelect={() => setSelectedInstanceId(inst.id)}
                onDelete={() => handleDelete(inst.id)}
              />
            ))
          )}
        </div>

        {/* Right: create form */}
        <div className="w-80 flex-shrink-0">
          <CreateForm
            versions={releaseVersions}
            defaultRam={ram}
            onCreate={(inst) => {
              addInstance(inst)
              setSelectedInstanceId(inst.id)
            }}
          />
        </div>
      </div>
    </div>
  )
}
