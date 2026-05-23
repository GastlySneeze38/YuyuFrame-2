import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '@/api/client'
import { useStore } from '@/stores/useStore'
import type { Instance, Loader, SyncInstance } from '@/types'
import { ModsContent } from '@/pages/Mods'

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
    <div className="flex flex-col gap-4">
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
        autoFocus
      />

      <div className="flex gap-3">
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

// ── Create modal ──────────────────────────────────────────────────────────────

function CreateModal({
  versions,
  defaultRam,
  onClose,
  onCreate,
}: {
  versions: string[]
  defaultRam: number
  onClose: () => void
  onCreate: (instance: Instance) => void
}) {
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center"
      style={{ background: 'rgba(0,0,0,0.6)', backdropFilter: 'blur(4px)' }}
      onClick={(e) => { if (e.target === e.currentTarget) onClose() }}
    >
      <div
        className="w-full max-w-md rounded-2xl p-6 flex flex-col gap-5"
        style={{ background: '#111118', border: '1px solid rgba(75,63,207,0.3)', boxShadow: '0 24px 80px rgba(0,0,0,0.6)' }}
      >
        <div className="flex items-center justify-between">
          <p className="font-bold text-white" style={{ fontSize: 15 }}>Nouvelle instance</p>
          <button
            onClick={onClose}
            className="flex h-7 w-7 items-center justify-center rounded-lg transition-all duration-150"
            style={{ color: 'rgba(255,255,255,0.3)', background: 'rgba(255,255,255,0.05)' }}
            onMouseEnter={(e) => { e.currentTarget.style.color = 'rgba(255,255,255,0.7)'; e.currentTarget.style.background = 'rgba(255,255,255,0.1)' }}
            onMouseLeave={(e) => { e.currentTarget.style.color = 'rgba(255,255,255,0.3)'; e.currentTarget.style.background = 'rgba(255,255,255,0.05)' }}
          >
            <svg viewBox="0 0 24 24" fill="currentColor" width={14} height={14}>
              <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z" />
            </svg>
          </button>
        </div>
        <CreateForm versions={versions} defaultRam={defaultRam} onCreate={(inst) => { onCreate(inst); onClose() }} />
      </div>
    </div>
  )
}

// ── Instance card ─────────────────────────────────────────────────────────────

function InstanceCard({
  instance,
  selected,
  onSelect,
  onToggleFavorite,
  onDelete,
}: {
  instance: Instance
  selected: boolean
  onSelect: () => void
  onToggleFavorite: () => void
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
      onMouseEnter={(e) => { if (!selected) e.currentTarget.style.background = 'rgba(255,255,255,0.06)' }}
      onMouseLeave={(e) => { if (!selected) e.currentTarget.style.background = 'rgba(255,255,255,0.04)' }}
    >
      <div
        className="flex items-center justify-center rounded-xl flex-shrink-0"
        style={{ width: 38, height: 38, background: selected ? 'rgba(75,63,207,0.3)' : 'rgba(255,255,255,0.05)', fontSize: 16 }}
      >
        🧱
      </div>

      <div className="min-w-0 flex-1">
        <p className="font-bold truncate" style={{ fontSize: 13, color: selected ? 'white' : 'rgba(255,255,255,0.85)' }}>
          {instance.name}
        </p>
        <div className="flex items-center gap-2 mt-0.5">
          <span style={{ fontSize: 11, color: 'rgba(255,255,255,0.3)' }}>{instance.mc_version}</span>
          <span style={{ fontSize: 10, color: loaderColor(instance.loader), fontWeight: 600 }}>{instance.loader}</span>
          <span style={{ fontSize: 10, color: 'rgba(255,255,255,0.2)' }}>{formatRam(instance.ram_mb)}</span>
        </div>
      </div>

      <div className="flex items-center gap-0.5" onClick={(e) => e.stopPropagation()}>
        {/* Star */}
        <button
          onClick={onToggleFavorite}
          className="flex h-8 w-8 items-center justify-center rounded-xl transition-all duration-150"
          title={instance.favorite ? 'Retirer des favoris' : 'Ajouter aux favoris'}
          style={{ color: instance.favorite ? '#facc15' : 'rgba(255,255,255,0.2)', background: 'transparent' }}
          onMouseEnter={(e) => { e.currentTarget.style.color = instance.favorite ? '#fde047' : 'rgba(255,255,255,0.5)' }}
          onMouseLeave={(e) => { e.currentTarget.style.color = instance.favorite ? '#facc15' : 'rgba(255,255,255,0.2)' }}
        >
          <svg viewBox="0 0 24 24" fill={instance.favorite ? 'currentColor' : 'none'} stroke="currentColor" strokeWidth={instance.favorite ? 0 : 1.5} width={15} height={15}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M11.48 3.499a.562.562 0 011.04 0l2.125 5.111a.563.563 0 00.475.345l5.518.442c.499.04.701.663.321.988l-4.204 3.602a.563.563 0 00-.182.557l1.285 5.385a.562.562 0 01-.84.61l-4.725-2.885a.563.563 0 00-.586 0L6.982 20.54a.562.562 0 01-.84-.61l1.285-5.386a.562.562 0 00-.182-.557l-4.204-3.602a.563.563 0 01.321-.988l5.518-.442a.563.563 0 00.475-.345L11.48 3.5z" />
          </svg>
        </button>

        {/* Delete */}
        {confirm ? (
          <div className="flex items-center gap-1">
            <button onClick={() => onDelete()} style={{ fontSize: 11, fontWeight: 600, color: 'rgb(248,113,113)', background: 'rgba(200,50,50,0.15)', borderRadius: 8, padding: '4px 8px' }}>Supprimer</button>
            <button onClick={() => setConfirm(false)} style={{ fontSize: 11, color: 'rgba(255,255,255,0.4)', background: 'rgba(255,255,255,0.06)', borderRadius: 8, padding: '4px 8px' }}>Annuler</button>
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

// ── Sync panel ────────────────────────────────────────────────────────────────

function formatDate(ts: number) {
  return new Date(ts * 1000).toLocaleString('fr-FR', {
    day: '2-digit', month: '2-digit', year: '2-digit',
    hour: '2-digit', minute: '2-digit',
  })
}

function SyncPanel({ instance, isLoggedIn }: { instance: Instance; isLoggedIn: boolean }) {
  const [cloudInstances, setCloudInstances] = useState<SyncInstance[]>([])
  const [loading, setLoading] = useState(false)
  const [pushing, setPushing] = useState(false)
  const [pullingId, setPullingId] = useState<number | null>(null)
  const [deletingId, setDeletingId] = useState<number | null>(null)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const loaded = useRef(false)

  const cloudForThisInstance = cloudInstances.find(
    (ci) => ci.instance_name === instance.name,
  )

  useEffect(() => {
    loaded.current = false
  }, [instance.id])

  useEffect(() => {
    if (!isLoggedIn || loaded.current) return
    loaded.current = true
    setLoading(true)
    api.sync.list()
      .then(setCloudInstances)
      .catch((e) => setError(e instanceof Error ? e.message : String(e)))
      .finally(() => setLoading(false))
  }, [instance.id, isLoggedIn])

  const notify = (msg: string) => {
    setSuccess(msg)
    setTimeout(() => setSuccess(''), 3000)
  }

  const handlePush = async () => {
    setPushing(true)
    setError('')
    try {
      const updated = await api.sync.push(instance.id)
      setCloudInstances((prev) => {
        const idx = prev.findIndex((ci) => ci.id === updated.id)
        return idx >= 0 ? prev.map((ci, i) => (i === idx ? updated : ci)) : [updated, ...prev]
      })
      notify('Synchronisé vers le cloud !')
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setPushing(false)
    }
  }

  const handlePull = async (syncId: number) => {
    setPullingId(syncId)
    setError('')
    try {
      await api.sync.pull(syncId, instance.id)
      notify('Données tirées depuis le cloud !')
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setPullingId(null)
    }
  }

  const handleDelete = async (syncId: number) => {
    setDeletingId(syncId)
    setError('')
    try {
      await api.sync.delete(syncId)
      setCloudInstances((prev) => prev.filter((ci) => ci.id !== syncId))
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setDeletingId(null)
    }
  }

  if (!isLoggedIn) {
    return (
      <div className="flex flex-1 flex-col items-center justify-center gap-3 p-6">
        <div style={{ fontSize: 28, opacity: 0.35 }}>🔒</div>
        <p style={{ fontSize: 13, color: 'rgba(255,255,255,0.35)', fontWeight: 600, textAlign: 'center' }}>
          Connecte-toi à YuyuFrame<br />pour accéder à la synchronisation
        </p>
      </div>
    )
  }

  return (
    <div className="flex flex-1 flex-col overflow-y-auto p-4 gap-4">
      {/* Push section */}
      <div
        className="rounded-2xl p-4 flex flex-col gap-3"
        style={{ background: 'rgba(75,63,207,0.08)', border: '1px solid rgba(75,63,207,0.2)' }}
      >
        <div className="flex items-center justify-between">
          <div>
            <p className="font-bold text-white" style={{ fontSize: 13 }}>Cette instance</p>
            {cloudForThisInstance ? (
              <p style={{ fontSize: 11, color: 'rgba(255,255,255,0.3)', marginTop: 2 }}>
                Dernière sync : {formatDate(cloudForThisInstance.updated_at)}
              </p>
            ) : (
              <p style={{ fontSize: 11, color: 'rgba(255,255,255,0.25)', marginTop: 2 }}>
                Pas encore synchronisée
              </p>
            )}
          </div>
          <button
            onClick={handlePush}
            disabled={pushing}
            className="flex items-center gap-2 font-semibold text-white transition-all duration-150 active:scale-95"
            style={{
              height: 34, padding: '0 14px', borderRadius: 10, fontSize: 12,
              background: pushing ? 'rgba(40,38,65,0.7)' : '#4B3FCF',
              boxShadow: pushing ? 'none' : '0 4px 16px rgba(75,63,207,0.3)',
              cursor: pushing ? 'not-allowed' : 'pointer',
            }}
            onMouseEnter={(e) => { if (!pushing) e.currentTarget.style.background = '#6155e8' }}
            onMouseLeave={(e) => { if (!pushing) e.currentTarget.style.background = '#4B3FCF' }}
          >
            {pushing ? (
              <span className="h-3 w-3 animate-spin rounded-full border-2" style={{ borderColor: 'rgba(255,255,255,0.2)', borderTopColor: 'white' }} />
            ) : (
              <svg viewBox="0 0 24 24" fill="currentColor" width={13} height={13}>
                <path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z" />
              </svg>
            )}
            {pushing ? 'Envoi...' : 'Pousser'}
          </button>
        </div>
      </div>

      {/* Feedback */}
      {success && (
        <p style={{ fontSize: 12, color: 'rgb(74,222,128)', fontWeight: 600, textAlign: 'center' }}>
          {success}
        </p>
      )}
      {error && (
        <p style={{ fontSize: 12, color: 'rgb(248,113,113)' }}>{error}</p>
      )}

      {/* Cloud list */}
      <div>
        <p className="font-semibold mb-2" style={{ fontSize: 10, color: 'rgba(255,255,255,0.3)', letterSpacing: '0.1em', textTransform: 'uppercase' }}>
          Cloud
        </p>

        {loading ? (
          <div className="flex justify-center py-6">
            <span className="h-6 w-6 animate-spin rounded-full border-2" style={{ borderColor: 'rgba(255,255,255,0.08)', borderTopColor: 'rgba(75,63,207,0.8)' }} />
          </div>
        ) : cloudInstances.length === 0 ? (
          <p style={{ fontSize: 12, color: 'rgba(255,255,255,0.2)', textAlign: 'center', paddingTop: 16 }}>
            Aucune instance synchronisée
          </p>
        ) : (
          <div className="flex flex-col gap-2">
            {cloudInstances.map((ci) => {
              const isCurrent = ci.instance_name === instance.name
              const isPulling = pullingId === ci.id
              const isDeleting = deletingId === ci.id
              return (
                <div
                  key={ci.id}
                  className="flex items-center gap-3 rounded-xl px-3 py-2.5"
                  style={{
                    background: isCurrent ? 'rgba(75,63,207,0.12)' : 'rgba(255,255,255,0.04)',
                    border: `1px solid ${isCurrent ? 'rgba(75,63,207,0.3)' : 'rgba(255,255,255,0.06)'}`,
                  }}
                >
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <p className="font-semibold truncate" style={{ fontSize: 12, color: 'rgba(255,255,255,0.85)' }}>
                        {ci.instance_name}
                      </p>
                      {isCurrent && (
                        <span style={{ fontSize: 9, fontWeight: 700, color: '#818cf8', background: 'rgba(75,63,207,0.2)', padding: '1px 6px', borderRadius: 4, letterSpacing: '0.05em' }}>
                          ACTUELLE
                        </span>
                      )}
                    </div>
                    <div className="flex items-center gap-2 mt-0.5">
                      <span style={{ fontSize: 10, color: 'rgba(255,255,255,0.25)' }}>{ci.mc_version}</span>
                      <span style={{ fontSize: 10, color: 'rgba(255,255,255,0.2)' }}>{ci.loader}</span>
                      {ci.has_data && (
                        <span style={{ fontSize: 10, color: 'rgba(255,255,255,0.2)' }}>{formatDate(ci.updated_at)}</span>
                      )}
                      {!ci.has_data && (
                        <span style={{ fontSize: 10, color: 'rgba(255,180,0,0.5)' }}>Pas de données</span>
                      )}
                    </div>
                  </div>

                  <div className="flex items-center gap-1">
                    {/* Pull */}
                    <button
                      onClick={() => handlePull(ci.id)}
                      disabled={isPulling || !ci.has_data}
                      title={ci.has_data ? `Tirer "${ci.instance_name}" dans cette instance` : 'Aucune donnée à tirer'}
                      className="flex items-center gap-1.5 font-semibold transition-all duration-150"
                      style={{
                        height: 28, padding: '0 10px', borderRadius: 8, fontSize: 11,
                        background: isPulling ? 'rgba(40,38,65,0.7)' : 'rgba(75,63,207,0.2)',
                        color: ci.has_data ? 'rgba(255,255,255,0.7)' : 'rgba(255,255,255,0.2)',
                        cursor: (isPulling || !ci.has_data) ? 'not-allowed' : 'pointer',
                        border: '1px solid rgba(75,63,207,0.25)',
                      }}
                      onMouseEnter={(e) => { if (ci.has_data && !isPulling) e.currentTarget.style.background = 'rgba(75,63,207,0.35)' }}
                      onMouseLeave={(e) => { if (ci.has_data && !isPulling) e.currentTarget.style.background = 'rgba(75,63,207,0.2)' }}
                    >
                      {isPulling ? (
                        <span className="h-3 w-3 animate-spin rounded-full border-2" style={{ borderColor: 'rgba(255,255,255,0.2)', borderTopColor: 'white' }} />
                      ) : (
                        <svg viewBox="0 0 24 24" fill="currentColor" width={11} height={11}>
                          <path d="M5 15H9v3h10V6H9v3H5v6zm4-4v-2h8v6H9v-2H7v-2h2z" />
                        </svg>
                      )}
                      Tirer
                    </button>

                    {/* Delete */}
                    <button
                      onClick={() => handleDelete(ci.id)}
                      disabled={isDeleting}
                      title="Supprimer ce sync"
                      className="flex h-7 w-7 items-center justify-center rounded-lg transition-all duration-150"
                      style={{ color: 'rgba(255,255,255,0.2)', background: 'transparent', cursor: isDeleting ? 'not-allowed' : 'pointer' }}
                      onMouseEnter={(e) => { if (!isDeleting) { e.currentTarget.style.color = 'rgb(248,113,113)'; e.currentTarget.style.background = 'rgba(200,50,50,0.12)' } }}
                      onMouseLeave={(e) => { e.currentTarget.style.color = 'rgba(255,255,255,0.2)'; e.currentTarget.style.background = 'transparent' }}
                    >
                      <svg viewBox="0 0 24 24" fill="currentColor" width={14} height={14}>
                        <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z" />
                      </svg>
                    </button>
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </div>

      <p style={{ fontSize: 10, color: 'rgba(255,255,255,0.15)', textAlign: 'center', marginTop: 4 }}>
        Configs + 3 saves max synchronisés
      </p>
    </div>
  )
}

// ── Right panel with tabs ──────────────────────────────────────────────────────

type RightTab = 'mods' | 'sync'

function RightPanel({ instance, isLoggedIn }: { instance: Instance; isLoggedIn: boolean }) {
  const [tab, setTab] = useState<RightTab>('mods')

  useEffect(() => {
    setTab('mods')
  }, [instance.id])

  return (
    <div className="flex flex-1 flex-col overflow-hidden">
      {/* Tab bar */}
      <div
        className="flex flex-shrink-0 items-center gap-1 px-3 pt-2 pb-0"
        style={{ borderBottom: '1px solid rgba(255,255,255,0.06)' }}
      >
        {(['mods', 'sync'] as RightTab[]).map((t) => (
          <button
            key={t}
            onClick={() => setTab(t)}
            className="font-semibold transition-all duration-150"
            style={{
              fontSize: 12,
              padding: '6px 14px',
              borderRadius: '8px 8px 0 0',
              background: 'transparent',
              color: tab === t ? 'white' : 'rgba(255,255,255,0.35)',
              borderBottom: tab === t ? '2px solid #4B3FCF' : '2px solid transparent',
            }}
          >
            {t === 'mods' ? 'Mods' : 'Sync'}
            {t === 'sync' && (
              <span style={{ marginLeft: 5, fontSize: 9, fontWeight: 700, color: '#818cf8', background: 'rgba(75,63,207,0.2)', padding: '1px 5px', borderRadius: 4 }}>
                PREMIUM
              </span>
            )}
          </button>
        ))}
      </div>

      {/* Content */}
      <div className="flex flex-1 flex-col overflow-hidden">
        {tab === 'mods' ? (
          <ModsContent instance={instance} />
        ) : (
          <SyncPanel instance={instance} isLoggedIn={isLoggedIn} />
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
    instances, setInstances, addInstance, updateInstance, removeInstance,
    selectedInstanceId, setSelectedInstanceId,
    defaultRam,
    yuyuToken,
  } = useStore()

  const [loading, setLoading] = useState(true)
  const [showCreate, setShowCreate] = useState(false)
  const [othersExpanded, setOthersExpanded] = useState(true)
  const loaded = useRef(false)

  const selectedInstance = instances.find((i) => i.id === selectedInstanceId) ?? null
  const favorites = instances.filter((i) => i.favorite)
  const others = instances.filter((i) => !i.favorite)

  useEffect(() => {
    if (loaded.current) return
    loaded.current = true

    Promise.all([
      api.instances.list(),
      versions.length === 0 ? api.versions.list() : Promise.resolve(null),
    ]).then(([insts, vers]) => {
      setInstances(insts)
      if (vers) setVersions(vers)
      if (insts.some((i) => i.favorite)) setOthersExpanded(false)
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

  const handleToggleFavorite = async (id: string) => {
    try {
      const updated = await api.instances.toggleFavorite(id)
      updateInstance(updated)
    } catch { /* ignore */ }
  }

  return (
    <div className="flex h-full flex-col" style={{ background: '#09090D', color: 'white' }}>

      {/* Header */}
      <div
        className="flex flex-shrink-0 items-center gap-3 px-5 py-3"
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

        <h1 className="font-black text-white" style={{ fontSize: 16, letterSpacing: '-0.01em' }}>Instances</h1>
      </div>

      {/* Body: sidebar + mods panel */}
      <div className="flex flex-1 overflow-hidden">

        {/* Left sidebar — instance list */}
        <div
          className="flex flex-col overflow-hidden"
          style={{
            width: 260,
            flexShrink: 0,
            borderRight: '1px solid rgba(255,255,255,0.06)',
          }}
        >
          {/* Scrollable list */}
          <div className="flex flex-1 flex-col overflow-y-auto p-3">
            {loading ? (
              <div className="flex h-40 items-center justify-center">
                <span className="h-7 w-7 animate-spin rounded-full border-2" style={{ borderColor: 'rgba(255,255,255,0.08)', borderTopColor: 'rgba(75,63,207,0.8)' }} />
              </div>
            ) : instances.length === 0 ? (
              <div className="flex h-full flex-col items-center justify-center gap-2">
                <div style={{ fontSize: 32 }}>🧱</div>
                <p style={{ fontSize: 13, color: 'rgba(255,255,255,0.3)', fontWeight: 600, textAlign: 'center' }}>Aucune instance</p>
              </div>
            ) : (
              <>
                {/* Favoris */}
                {favorites.length > 0 && (
                  <div className="mb-1">
                    <p className="px-1 pb-1.5 text-xs font-semibold" style={{ color: '#facc15', letterSpacing: '0.08em', textTransform: 'uppercase' }}>
                      ★ Favoris
                    </p>
                    <div className="flex flex-col gap-2">
                      {favorites.map((inst) => (
                        <InstanceCard
                          key={inst.id}
                          instance={inst}
                          selected={inst.id === selectedInstanceId}
                          onSelect={() => setSelectedInstanceId(inst.id)}
                          onToggleFavorite={() => handleToggleFavorite(inst.id)}
                          onDelete={() => handleDelete(inst.id)}
                        />
                      ))}
                    </div>
                  </div>
                )}

                {/* Autres */}
                {others.length > 0 && (
                  <div>
                    <button
                      onClick={() => setOthersExpanded((v) => !v)}
                      className="flex w-full items-center gap-1.5 px-1 pb-1.5"
                    >
                      <svg
                        viewBox="0 0 24 24" fill="currentColor" width={10} height={10}
                        style={{ color: 'rgba(255,255,255,0.3)', transition: 'transform 0.15s', transform: othersExpanded ? 'rotate(90deg)' : 'rotate(0deg)' }}
                      >
                        <path d="M8 5v14l11-7z" />
                      </svg>
                      <p className="text-xs font-semibold" style={{ color: 'rgba(255,255,255,0.3)', letterSpacing: '0.08em', textTransform: 'uppercase' }}>
                        Autres ({others.length})
                      </p>
                    </button>
                    {othersExpanded && (
                      <div className="flex flex-col gap-2">
                        {others.map((inst) => (
                          <InstanceCard
                            key={inst.id}
                            instance={inst}
                            selected={inst.id === selectedInstanceId}
                            onSelect={() => setSelectedInstanceId(inst.id)}
                            onToggleFavorite={() => handleToggleFavorite(inst.id)}
                            onDelete={() => handleDelete(inst.id)}
                          />
                        ))}
                      </div>
                    )}
                  </div>
                )}
              </>
            )}
          </div>

          {/* Fixed bottom button */}
          <div className="flex-shrink-0 p-3" style={{ borderTop: '1px solid rgba(255,255,255,0.06)' }}>
            <button
              onClick={() => setShowCreate(true)}
              className="w-full flex items-center justify-center gap-2 font-bold text-white transition-all duration-200 active:scale-95"
              style={{
                height: 44, borderRadius: 12, fontSize: 13,
                background: '#4B3FCF',
                boxShadow: '0 4px 20px rgba(75,63,207,0.3)',
              }}
              onMouseEnter={(e) => { e.currentTarget.style.background = '#6155e8' }}
              onMouseLeave={(e) => { e.currentTarget.style.background = '#4B3FCF' }}
            >
              <svg viewBox="0 0 24 24" fill="currentColor" width={15} height={15}>
                <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z" />
              </svg>
              Nouvelle instance
            </button>
          </div>
        </div>

        {/* Right panel — mods / sync */}
        <div className="flex flex-1 flex-col overflow-hidden">
          {selectedInstance ? (
            <RightPanel instance={selectedInstance} isLoggedIn={yuyuToken !== null} />
          ) : (
            <div className="flex flex-1 flex-col items-center justify-center gap-3">
              <div style={{ fontSize: 32, opacity: 0.4 }}>←</div>
              <p style={{ fontSize: 14, color: 'rgba(255,255,255,0.3)', fontWeight: 600 }}>
                Sélectionne une instance
              </p>
              <p style={{ fontSize: 12, color: 'rgba(255,255,255,0.15)' }}>
                Les mods s'afficheront ici
              </p>
            </div>
          )}
        </div>
      </div>

      {/* Create modal */}
      {showCreate && (
        <CreateModal
          versions={releaseVersions}
          defaultRam={defaultRam}
          onClose={() => setShowCreate(false)}
          onCreate={(inst) => {
            addInstance(inst)
            setSelectedInstanceId(inst.id)
          }}
        />
      )}
    </div>
  )
}
