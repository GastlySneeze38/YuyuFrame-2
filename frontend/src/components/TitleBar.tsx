import { useStore } from '@/stores/useStore'

export function TitleBar() {
  const { theme, toggleTheme, username } = useStore()

  const minimize = () => window.api?.minimize()
  const maximize = () => window.api?.maximize()
  const close = () => window.api?.close()

  return (
    <div className="drag-region flex h-10 items-center justify-between border-b border-border bg-bg-secondary px-4">
      {/* Logo */}
      <div className="no-drag flex items-center gap-2">
        <div className="h-5 w-5 rounded-sm bg-accent" />
        <span className="text-sm font-bold tracking-widest text-txt-primary">YUYUFRAME</span>
      </div>

      {/* Center: user + theme */}
      <div className="no-drag flex items-center gap-3">
        {username && (
          <span className="text-xs text-txt-secondary">
            Connecté en tant que <span className="text-accent font-medium">{username}</span>
          </span>
        )}
        <button
          onClick={toggleTheme}
          className="rounded px-2 py-1 text-xs text-txt-secondary transition-theme hover:bg-bg-card hover:text-txt-primary"
        >
          {theme === 'chill' ? '❄️ Chill' : '🎮 Gamer'}
        </button>
      </div>

      {/* Window controls */}
      <div className="no-drag flex items-center gap-1">
        <button
          onClick={minimize}
          className="flex h-7 w-7 items-center justify-center rounded text-txt-secondary transition hover:bg-bg-card hover:text-txt-primary"
        >
          <svg width="10" height="1" viewBox="0 0 10 1" fill="currentColor">
            <rect width="10" height="1" />
          </svg>
        </button>
        <button
          onClick={maximize}
          className="flex h-7 w-7 items-center justify-center rounded text-txt-secondary transition hover:bg-bg-card hover:text-txt-primary"
        >
          <svg width="9" height="9" viewBox="0 0 9 9" fill="none" stroke="currentColor" strokeWidth="1">
            <rect x="0.5" y="0.5" width="8" height="8" />
          </svg>
        </button>
        <button
          onClick={close}
          className="flex h-7 w-7 items-center justify-center rounded text-txt-secondary transition hover:bg-red-500 hover:text-white"
        >
          <svg width="9" height="9" viewBox="0 0 9 9" fill="none" stroke="currentColor" strokeWidth="1.2">
            <line x1="0" y1="0" x2="9" y2="9" />
            <line x1="9" y1="0" x2="0" y2="9" />
          </svg>
        </button>
      </div>
    </div>
  )
}
