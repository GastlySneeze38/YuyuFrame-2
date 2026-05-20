import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { Loader, Theme, Version, Account } from '@/types'

interface Store {
  // ── YuyuFrame session (NOT persisted — requires password on each start) ──
  yuyuToken: string | null
  yuyuUsername: string | null
  setYuyuSession: (token: string, username: string) => void
  clearYuyuSession: () => void

  // ── Theme ──────────────────────────────────────────────────────────────────
  theme: Theme
  setTheme: (t: Theme) => void
  toggleTheme: () => void

  // ── Active Minecraft account ───────────────────────────────────────────────
  username: string | null
  uuid: string | null
  setUser: (username: string, uuid: string) => void
  clearUser: () => void

  // ── Minecraft account list (synced from backend after yuyu login) ──────────
  accounts: Account[]
  setAccounts: (accounts: Account[]) => void
  addAccount: (username: string, uuid: string) => void
  removeAccount: (uuid: string) => void
  switchAccount: (uuid: string) => void

  // ── Versions ───────────────────────────────────────────────────────────────
  versions: Version[]
  setVersions: (v: Version[]) => void

  selectedVersion: string
  setSelectedVersion: (v: string) => void

  selectedLoader: Loader
  setSelectedLoader: (l: Loader) => void

  // ── Settings (persisted) ──────────────────────────────────────────────────
  ram: number
  setRam: (r: number) => void

  javaPath: string
  setJavaPath: (p: string) => void

  minecraftPath: string
  setMinecraftPath: (p: string) => void

  brightness: number
  setBrightness: (b: number) => void

  // ── Game state ─────────────────────────────────────────────────────────────
  gameRunning: boolean
  setGameRunning: (r: boolean) => void
}

export const useStore = create<Store>()(
  persist(
    (set, get) => ({
      // YuyuFrame session
      yuyuToken: null,
      yuyuUsername: null,
      setYuyuSession: (token, username) => {
        set({ yuyuToken: token, yuyuUsername: username })
      },
      clearYuyuSession: () => {
        set({ yuyuToken: null, yuyuUsername: null, accounts: [], username: null, uuid: null })
      },

      // Theme
      theme: 'chill',
      setTheme: (theme) => {
        set({ theme })
        document.documentElement.setAttribute('data-theme', theme)
      },
      toggleTheme: () => {
        const next = get().theme === 'chill' ? 'gamer' : 'chill'
        get().setTheme(next)
      },

      // Active MC account
      username: null,
      uuid: null,
      setUser: (username, uuid) => set({ username, uuid }),
      clearUser: () => set({ username: null, uuid: null }),

      // MC account list
      accounts: [],
      setAccounts: (accounts) => set({ accounts }),
      addAccount: (username, uuid) => {
        const accounts = get().accounts
        const idx = accounts.findIndex((a) => a.uuid === uuid)
        const next =
          idx >= 0
            ? accounts.map((a, i) => (i === idx ? { username, uuid } : a))
            : accounts.length < 2
            ? [...accounts, { username, uuid }]
            : accounts
        set({ accounts: next, username, uuid })
      },
      removeAccount: (targetUuid) => {
        const accounts = get().accounts.filter((a) => a.uuid !== targetUuid)
        if (get().uuid === targetUuid) {
          const other = accounts[0] ?? null
          set({ accounts, username: other?.username ?? null, uuid: other?.uuid ?? null })
        } else {
          set({ accounts })
        }
      },
      switchAccount: (targetUuid) => {
        const account = get().accounts.find((a) => a.uuid === targetUuid)
        if (account) set({ username: account.username, uuid: account.uuid })
      },

      // Versions
      versions: [],
      setVersions: (versions) => set({ versions }),

      selectedVersion: '',
      setSelectedVersion: (selectedVersion) => set({ selectedVersion }),

      selectedLoader: 'vanilla',
      setSelectedLoader: (selectedLoader) => set({ selectedLoader }),

      // Settings
      ram: 4096,
      setRam: (ram) => set({ ram }),

      javaPath: '',
      setJavaPath: (javaPath) => set({ javaPath }),

      minecraftPath: '',
      setMinecraftPath: (minecraftPath) => set({ minecraftPath }),

      brightness: 100,
      setBrightness: (brightness) => set({ brightness }),

      // Game
      gameRunning: false,
      setGameRunning: (gameRunning) => set({ gameRunning }),
    }),
    {
      name: 'yuyuframe-store',
      partialize: (s) => ({
        theme: s.theme,
        selectedVersion: s.selectedVersion,
        selectedLoader: s.selectedLoader,
        ram: s.ram,
        javaPath: s.javaPath,
        minecraftPath: s.minecraftPath,
        brightness: s.brightness,
        username: s.username,
        uuid: s.uuid,
      }),
    }
  )
)
