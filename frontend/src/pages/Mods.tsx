import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '@/api/client'
import type { Mod } from '@/types'

function formatSize(bytes: number): string {
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} Ko`
  return `${(bytes / (1024 * 1024)).toFixed(1)} Mo`
}

function displayName(name: string): string {
  return name.replace(/\.jar(\.disabled)?$/, '')
}

export default function Mods() {
  const navigate = useNavigate()
  const [mods, setMods] = useState<Mod[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [uploading, setUploading] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

  const loadMods = async () => {
    setLoading(true)
    setError('')
    try {
      setMods(await api.mods.list())
    } catch {
      setError('Impossible de charger les mods')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadMods() }, [])

  const handleToggle = async (mod: Mod) => {
    try {
      const updated = await api.mods.toggle(mod.name)
      setMods((prev) => prev.map((m) => m.name === mod.name ? updated : m))
    } catch { /* ignore */ }
  }

  const handleDelete = async (name: string) => {
    try {
      await api.mods.delete(name)
      setMods((prev) => prev.filter((m) => m.name !== name))
    } catch { /* ignore */ }
  }

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    setUploading(true)
    try {
      const newMod = await api.mods.upload(file)
      setMods((prev) =>
        [...prev.filter((m) => m.name !== newMod.name), newMod]
          .sort((a, b) => a.name.toLowerCase().localeCompare(b.name.toLowerCase()))
      )
    } catch {
      setError("Erreur lors de l'import du mod")
    } finally {
      setUploading(false)
      if (fileInputRef.current) fileInputRef.current.value = ''
    }
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

        <div className="flex-1">
          <h1 className="font-black text-white" style={{ fontSize: 18, letterSpacing: '-0.01em' }}>Mods</h1>
          <p style={{ fontSize: 11, color: 'rgba(255,255,255,0.3)', marginTop: 1 }}>
            {mods.length} mod{mods.length !== 1 ? 's' : ''} installé{mods.length !== 1 ? 's' : ''}
          </p>
        </div>

        <button
          onClick={() => fileInputRef.current?.click()}
          disabled={uploading}
          className="flex items-center gap-2 font-semibold transition-all duration-200 active:scale-95"
          style={{
            height: 36,
            paddingLeft: 14,
            paddingRight: 14,
            borderRadius: 10,
            fontSize: 13,
            background: uploading ? 'rgba(40,38,65,0.7)' : '#4B3FCF',
            boxShadow: uploading ? 'none' : '0 4px 20px rgba(75,63,207,0.35)',
            cursor: uploading ? 'not-allowed' : 'pointer',
          }}
          onMouseEnter={(e) => { if (!uploading) e.currentTarget.style.background = '#6155e8' }}
          onMouseLeave={(e) => { if (!uploading) e.currentTarget.style.background = '#4B3FCF' }}
        >
          <svg viewBox="0 0 24 24" fill="currentColor" width={15} height={15}>
            <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z" />
          </svg>
          {uploading ? 'Import...' : 'Ajouter'}
        </button>

        <input
          ref={fileInputRef}
          type="file"
          accept=".jar"
          className="hidden"
          onChange={handleFileChange}
        />
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto px-6 py-4">
        {loading ? (
          <div className="flex h-full items-center justify-center">
            <span
              className="h-8 w-8 animate-spin rounded-full border-2"
              style={{ borderColor: 'rgba(255,255,255,0.08)', borderTopColor: 'rgba(75,63,207,0.8)' }}
            />
          </div>
        ) : error ? (
          <div className="flex h-full flex-col items-center justify-center gap-3">
            <span style={{ color: 'rgba(255,255,255,0.3)', fontSize: 13 }}>{error}</span>
            <button
              onClick={loadMods}
              style={{ fontSize: 12, color: '#7872e8', textDecoration: 'underline' }}
            >
              Réessayer
            </button>
          </div>
        ) : mods.length === 0 ? (
          <div className="flex h-full flex-col items-center justify-center gap-4">
            <div
              style={{
                width: 56, height: 56, borderRadius: 16,
                background: 'rgba(75,63,207,0.1)',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}
            >
              <svg viewBox="0 0 24 24" fill="rgba(75,63,207,0.55)" width={28} height={28}>
                <path d="M20.5 11H19V7c0-1.1-.9-2-2-2h-4V3.5C13 2.12 11.88 1 10.5 1S8 2.12 8 3.5V5H4c-1.1 0-1.99.9-1.99 2v3.8H3.5c1.49 0 2.7 1.21 2.7 2.7s-1.21 2.7-2.7 2.7H2V20c0 1.1.9 2 2 2h3.8v-1.5c0-1.49 1.21-2.7 2.7-2.7 1.49 0 2.7 1.21 2.7 2.7V22H17c1.1 0 2-.9 2-2v-4h1.5c1.38 0 2.5-1.12 2.5-2.5S21.88 11 20.5 11z" />
              </svg>
            </div>
            <div className="text-center">
              <p className="font-semibold" style={{ color: 'rgba(255,255,255,0.5)', fontSize: 14 }}>
                Aucun mod installé
              </p>
              <p style={{ color: 'rgba(255,255,255,0.2)', fontSize: 12, marginTop: 4 }}>
                Cliquez sur "Ajouter" pour importer un fichier .jar
              </p>
            </div>
          </div>
        ) : (
          <div className="flex flex-col gap-2">
            {mods.map((mod) => (
              <ModRow
                key={mod.name}
                mod={mod}
                onToggle={() => handleToggle(mod)}
                onDelete={() => handleDelete(mod.name)}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

function ModRow({
  mod,
  onToggle,
  onDelete,
}: {
  mod: Mod
  onToggle: () => void
  onDelete: () => void
}) {
  const [confirmDelete, setConfirmDelete] = useState(false)

  return (
    <div
      className="flex items-center gap-3 rounded-2xl px-4 py-3 transition-all duration-150"
      style={{
        background: mod.enabled ? 'rgba(255,255,255,0.04)' : 'rgba(255,255,255,0.018)',
        border: '1px solid rgba(255,255,255,0.06)',
        opacity: mod.enabled ? 1 : 0.6,
      }}
    >
      {/* Mod icon */}
      <div
        style={{
          width: 36, height: 36, borderRadius: 10, flexShrink: 0,
          background: mod.enabled ? 'rgba(75,63,207,0.15)' : 'rgba(255,255,255,0.05)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}
      >
        <svg viewBox="0 0 24 24" fill={mod.enabled ? 'rgba(120,110,230,0.8)' : 'rgba(255,255,255,0.2)'} width={18} height={18}>
          <path d="M20.5 11H19V7c0-1.1-.9-2-2-2h-4V3.5C13 2.12 11.88 1 10.5 1S8 2.12 8 3.5V5H4c-1.1 0-1.99.9-1.99 2v3.8H3.5c1.49 0 2.7 1.21 2.7 2.7s-1.21 2.7-2.7 2.7H2V20c0 1.1.9 2 2 2h3.8v-1.5c0-1.49 1.21-2.7 2.7-2.7 1.49 0 2.7 1.21 2.7 2.7V22H17c1.1 0 2-.9 2-2v-4h1.5c1.38 0 2.5-1.12 2.5-2.5S21.88 11 20.5 11z" />
        </svg>
      </div>

      {/* Name + size */}
      <div className="min-w-0 flex-1">
        <p
          className="truncate font-semibold"
          style={{ fontSize: 13, color: mod.enabled ? 'rgba(255,255,255,0.9)' : 'rgba(255,255,255,0.4)' }}
        >
          {displayName(mod.name)}
        </p>
        <p style={{ fontSize: 11, color: 'rgba(255,255,255,0.22)', marginTop: 1 }}>
          {formatSize(mod.size)}
        </p>
      </div>

      {/* Toggle switch */}
      <button
        onClick={onToggle}
        title={mod.enabled ? 'Désactiver' : 'Activer'}
        className="relative flex-shrink-0 transition-all duration-200"
        style={{
          width: 40, height: 22, borderRadius: 11,
          background: mod.enabled ? '#4B3FCF' : 'rgba(255,255,255,0.1)',
          border: 'none', cursor: 'pointer', flexShrink: 0,
        }}
      >
        <span
          className="absolute transition-all duration-200"
          style={{
            top: 3,
            left: mod.enabled ? 21 : 3,
            width: 16, height: 16,
            borderRadius: '50%',
            background: 'white',
            boxShadow: '0 1px 4px rgba(0,0,0,0.4)',
          }}
        />
      </button>

      {/* Delete */}
      {confirmDelete ? (
        <div className="flex flex-shrink-0 items-center gap-1">
          <button
            onClick={() => { onDelete(); setConfirmDelete(false) }}
            style={{
              fontSize: 11, fontWeight: 600,
              color: 'rgb(248,113,113)',
              background: 'rgba(200,50,50,0.15)',
              borderRadius: 8, padding: '4px 10px',
            }}
          >
            Supprimer
          </button>
          <button
            onClick={() => setConfirmDelete(false)}
            style={{
              fontSize: 11,
              color: 'rgba(255,255,255,0.4)',
              background: 'rgba(255,255,255,0.06)',
              borderRadius: 8, padding: '4px 10px',
            }}
          >
            Annuler
          </button>
        </div>
      ) : (
        <button
          onClick={() => setConfirmDelete(true)}
          title="Supprimer"
          className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-xl transition-all duration-150"
          style={{ color: 'rgba(255,255,255,0.2)' }}
          onMouseEnter={(e) => {
            e.currentTarget.style.color = 'rgb(248,113,113)'
            e.currentTarget.style.background = 'rgba(200,50,50,0.12)'
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.color = 'rgba(255,255,255,0.2)'
            e.currentTarget.style.background = 'transparent'
          }}
        >
          <svg viewBox="0 0 24 24" fill="currentColor" width={16} height={16}>
            <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z" />
          </svg>
        </button>
      )}
    </div>
  )
}
