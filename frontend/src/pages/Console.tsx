import { useEffect, useRef, useState } from 'react'
import { listen } from '@tauri-apps/api/event'
import { getCurrentWindow } from '@tauri-apps/api/window'

interface LogLine {
  id: number
  line: string
  level: 'out' | 'err'
}

function lineColor(line: string, level: 'out' | 'err'): string {
  if (/\bwarn(?:ing)?\b/i.test(line)) return 'rgba(250,204,21,0.82)'
  if (level === 'err' || /\berror\b|\bsevere\b/i.test(line)) return 'rgba(248,113,113,0.88)'
  return 'rgba(180,180,190,0.65)'
}

const MAX_LINES = 3000

export default function Console() {
  const [logs, setLogs] = useState<LogLine[]>([])
  const [running, setRunning] = useState(true)
  const [copied, setCopied] = useState(false)
  const bottomRef = useRef<HTMLDivElement>(null)
  const counterRef = useRef(0)
  // Ref pour accumulation entre renders — évite setState à chaque ligne
  const pendingRef = useRef<LogLine[]>([])
  const rafRef = useRef<number | null>(null)

  function addLine(line: string, level: 'out' | 'err') {
    pendingRef.current.push({ id: counterRef.current++, line, level })
    if (rafRef.current === null) {
      rafRef.current = requestAnimationFrame(() => {
        rafRef.current = null
        const batch = pendingRef.current.splice(0)
        if (batch.length > 0) {
          setLogs((prev) => {
            const next = [...prev, ...batch]
            return next.length > MAX_LINES ? next.slice(-MAX_LINES) : next
          })
        }
      })
    }
  }

  function copyAll() {
    const text = logs.map((l) => l.line).join('\n')
    navigator.clipboard.writeText(text).then(() => {
      setCopied(true)
      setTimeout(() => setCopied(false), 1500)
    })
  }

  useEffect(() => {
    const win = getCurrentWindow()
    const myShortId = win.label.replace('mc-console-', '')

    const unsubPromises = [
      listen<{ line: string; level: 'out' | 'err' }>('game_log', (e) => {
        addLine(e.payload.line, e.payload.level)
      }),
      listen<{ running: boolean; instance_id: string }>('game_state', (e) => {
        if (!e.payload.running && e.payload.instance_id.startsWith(myShortId)) {
          setRunning(false)
        }
      }),
    ]

    return () => {
      if (rafRef.current !== null) cancelAnimationFrame(rafRef.current)
      unsubPromises.forEach((p) => p.then((fn) => fn()))
    }
  }, [])

  useEffect(() => {
    const sel = window.getSelection()
    if (sel && !sel.isCollapsed) return
    bottomRef.current?.scrollIntoView({ behavior: 'instant' })
  }, [logs])

  return (
    <div className="flex h-screen flex-col" style={{ background: '#07070C', fontFamily: 'monospace' }}>

      {/* Drag bar + window controls */}
      <div
        data-tauri-drag-region
        className="flex flex-shrink-0 items-center"
        style={{ height: 36, background: '#09090D', borderBottom: '1px solid rgba(255,255,255,0.07)', userSelect: 'none' }}
      >
        {/* Titre (drag zone) */}
        <div
          data-tauri-drag-region
          className="flex flex-1 items-center gap-2 px-4"
          style={{ pointerEvents: 'none' }}
        >
          <div
            style={{
              width: 6, height: 6, borderRadius: '50%',
              background: running ? '#4ade80' : 'rgba(255,255,255,0.2)',
              boxShadow: running ? '0 0 5px #4ade80' : 'none',
            }}
          />
          <span style={{ fontSize: 11, color: running ? 'rgba(255,255,255,0.5)' : 'rgba(255,255,255,0.25)' }}>
            {running ? 'Minecraft en cours...' : 'Jeu terminé'}
          </span>
          <span style={{ fontSize: 11, color: 'rgba(255,255,255,0.15)' }}>{logs.length} lignes</span>
        </div>

        {/* Boutons — hors drag region (pointer-events explicite) */}
        <div className="flex items-center gap-0.5 px-2" style={{ pointerEvents: 'auto' }}>
          <button
            onClick={copyAll}
            disabled={logs.length === 0}
            style={{
              background: 'none', border: '1px solid rgba(255,255,255,0.12)', borderRadius: 4,
              color: copied ? '#4ade80' : 'rgba(255,255,255,0.4)',
              cursor: logs.length === 0 ? 'default' : 'pointer',
              fontSize: 11, padding: '1px 8px', marginRight: 6,
            }}
          >
            {copied ? '✓ Copié' : 'Copier'}
          </button>
          <button
            onClick={() => getCurrentWindow().minimize()}
            style={{ background: 'none', border: 'none', cursor: 'pointer', width: 28, height: 28, color: 'rgba(255,255,255,0.3)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
            onMouseEnter={(e) => { (e.currentTarget as HTMLElement).style.background = 'rgba(255,255,255,0.07)' }}
            onMouseLeave={(e) => { (e.currentTarget as HTMLElement).style.background = 'none' }}
          >
            <svg width="10" height="2" viewBox="0 0 10 2" fill="currentColor"><rect width="10" height="1.5" y="0.25" /></svg>
          </button>
          <button
            onClick={() => getCurrentWindow().close()}
            style={{ background: 'none', border: 'none', cursor: 'pointer', width: 28, height: 28, color: 'rgba(220,60,60,0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
            onMouseEnter={(e) => { (e.currentTarget as HTMLElement).style.background = 'rgba(220,45,45,0.2)'; (e.currentTarget as HTMLElement).style.color = 'rgb(245,80,80)' }}
            onMouseLeave={(e) => { (e.currentTarget as HTMLElement).style.background = 'none'; (e.currentTarget as HTMLElement).style.color = 'rgba(220,60,60,0.6)' }}
          >
            <svg width="9" height="9" viewBox="0 0 9 9" fill="none" stroke="currentColor" strokeWidth="1.5">
              <line x1="0.5" y1="0.5" x2="8.5" y2="8.5" /><line x1="8.5" y1="0.5" x2="0.5" y2="8.5" />
            </svg>
          </button>
        </div>
      </div>

      {/* Logs */}
      <div className="selectable min-h-0 flex-1 overflow-y-auto px-4 py-3" style={{ overflowAnchor: 'none' }}>
        {logs.length === 0 && (
          <p style={{ fontSize: 12, color: 'rgba(255,255,255,0.18)', margin: 0 }}>
            En attente des logs...
          </p>
        )}
        {logs.map((log) => (
          <div
            key={log.id}
            style={{
              color: lineColor(log.line, log.level),
              lineHeight: 1.55,
              whiteSpace: 'pre-wrap',
              wordBreak: 'break-all',
              fontSize: 11,
            }}
          >
            {log.line}
          </div>
        ))}
        <div ref={bottomRef} />
      </div>
    </div>
  )
}
