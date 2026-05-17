import { useNavigate, useLocation } from 'react-router-dom'
import { useStore } from '@/stores/useStore'

type NavItem = {
  path: string
  title: string
  disabled?: boolean
  icon: React.ReactNode
}

const items: NavItem[] = [
  {
    path: '/home',
    title: 'Jouer',
    icon: (
      <svg viewBox="0 0 24 24" fill="currentColor" className="h-5 w-5">
        <path d="M8 5v14l11-7z" />
      </svg>
    ),
  },
  {
    path: '/settings',
    title: 'Paramètres',
    icon: (
      <svg viewBox="0 0 24 24" fill="currentColor" className="h-5 w-5">
        <path d="M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z" />
      </svg>
    ),
  },
  {
    path: '/servers',
    title: 'Serveurs (bientôt)',
    disabled: true,
    icon: (
      <svg viewBox="0 0 24 24" fill="currentColor" className="h-5 w-5">
        <path d="M20 3H4v10c0 1.1.9 2 2 2h2v2H6v2h12v-2h-2v-2h2c1.1 0 2-.9 2-2V3zm-2 10H6V5h12v8z" />
      </svg>
    ),
  },
  {
    path: '/mods',
    title: 'Mods',
    icon: (
      <svg viewBox="0 0 24 24" fill="currentColor" className="h-5 w-5">
        <path d="M20.5 11H19V7c0-1.1-.9-2-2-2h-4V3.5C13 2.12 11.88 1 10.5 1S8 2.12 8 3.5V5H4c-1.1 0-1.99.9-1.99 2v3.8H3.5c1.49 0 2.7 1.21 2.7 2.7s-1.21 2.7-2.7 2.7H2V20c0 1.1.9 2 2 2h3.8v-1.5c0-1.49 1.21-2.7 2.7-2.7 1.49 0 2.7 1.21 2.7 2.7V22H17c1.1 0 2-.9 2-2v-4h1.5c1.38 0 2.5-1.12 2.5-2.5S21.88 11 20.5 11z" />
      </svg>
    ),
  },
]

export function Sidebar() {
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const { username } = useStore()

  return (
    <aside className="flex w-14 flex-col items-center gap-1.5 border-r border-border bg-bg-secondary py-3 transition-theme">
      {items.map(({ path, title, icon, disabled }) => (
        <button
          key={path}
          title={title}
          onClick={() => !disabled && navigate(path)}
          className={[
            'flex h-10 w-10 items-center justify-center rounded-xl transition-all duration-150',
            disabled
              ? 'cursor-not-allowed text-txt-secondary opacity-25'
              : pathname === path
              ? 'bg-accent text-white shadow-lg'
              : 'text-txt-secondary hover:bg-bg-card hover:text-txt-primary',
          ].join(' ')}
        >
          {icon}
        </button>
      ))}

      {/* Bottom: login button or logo */}
      <div className="mt-auto flex flex-col items-center gap-2 pb-1">
        {username ? (
          <button
            className="flex h-10 w-10 items-center justify-center rounded-xl transition-all duration-150 bg-accent text-xs font-black text-white hover:opacity-80"
            style={{ fontFamily: 'monospace' }}
            title={username}
            onClick={() => navigate('/login')}
          >
            {username[0].toUpperCase()}
          </button>
        ) : (
          <button
            title="Se connecter"
            onClick={() => navigate('/login')}
            className={[
              'flex h-10 w-10 items-center justify-center rounded-xl transition-all duration-150',
              pathname === '/login'
                ? 'bg-accent text-white'
                : 'text-txt-secondary hover:bg-bg-card hover:text-accent',
            ].join(' ')}
          >
            <svg viewBox="0 0 24 24" fill="currentColor" className="h-5 w-5">
              <path d="M11 7L9.6 8.4l2.6 2.6H2v2h10.2l-2.6 2.6L11 17l5-5-5-5zm9 12h-8v2h8c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2h-8v2h8v14z" />
            </svg>
          </button>
        )}
      </div>
    </aside>
  )
}
