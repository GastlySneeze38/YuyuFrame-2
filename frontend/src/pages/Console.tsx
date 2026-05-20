import { useEffect, useRef, useState } from 'react'
import { listen } from '@tauri-apps/api/event'

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

export default function Console() {
  const [logs, setLogs] = useState<LogLine[]>([])
  const [running, setRunning] = useState(true)
  const bottomRef = useRef<HTMLDivElement>(null)
  const counterRef = useRef(0)
  const bufferRef = useRef<LogLine[]>([])

  useEffect(() => {
    let rafId: number

    const flush = () => {
      if (bufferRef.current.length > 0) {
        const batch = bufferRef.current.splice(0)
        setLogs((prev) => {
          const next = [...prev, ...batch]
          return next.length > 5000 ? next.slice(-5000) : next
        })
      }
      rafId = requestAnimationFrame(flush)
    }
    rafId = requestAnimationFrame(flush)

    const subs = [
      listen<{ line: string; level: 'out' | 'err' }>('game_log', (e) => {
        bufferRef.current.push({ id: counterRef.current++, ...e.payload })
      }),
      listen<{ running: boolean }>('game_state', (e) => {
        if (!e.payload.running) setRunning(false)
      }),
    ]

    return () => {
      cancelAnimationFrame(rafId)
      subs.forEach((p) => p.then((unsub) => unsub()))
    }
  }, [])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'instant' })
  }, [logs])

  return (
    <div className="flex h-full flex-col" style={{ background: '#07070C', fontFamily: 'monospace' }}>
      {/* Status bar */}
      <div
        className="flex flex-shrink-0 items-center gap-2.5 px-4 py-2 text-xs"
        style={{ borderBottom: '1px solid rgba(255,255,255,0.07)' }}
      >
        <div
          className="h-1.5 w-1.5 rounded-full"
          style={{
            background: running ? '#4ade80' : 'rgba(255,255,255,0.2)',
            boxShadow: running ? '0 0 6px #4ade80' : 'none',
          }}
        />
        <span style={{ color: running ? 'rgba(255,255,255,0.55)' : 'rgba(255,255,255,0.28)' }}>
          {running ? 'Minecraft en cours...' : 'Jeu terminé'}
        </span>
        <span className="ml-auto" style={{ color: 'rgba(255,255,255,0.18)' }}>
          {logs.length} lignes
        </span>
      </div>

      {/* Log output */}
      <div className="min-h-0 flex-1 overflow-y-auto px-4 py-3">
        {logs.length === 0 && (
          <p className="text-xs" style={{ color: 'rgba(255,255,255,0.18)' }}>
            En attente des logs...
          </p>
        )}
        {logs.map((log) => (
          <div
            key={log.id}
            style={{
              color: lineColor(log.line, log.level),
              lineHeight: '1.55',
              whiteSpace: 'pre-wrap',
              wordBreak: 'break-all',
              fontSize: '11px',
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
