import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { Theme, Version, Account } from '@/types'

interface Store {
  theme: Theme
  setTheme: (t: Theme) => void
  toggleTheme: () => void

  username: string | null
  uuid: string | null
  setUser: (username: string, uuid: string) => void
  clearUser: () => void

  accounts: Account[]
  addAccount: (username: string, uuid: string) => void
  removeAccount: (uuid: string) => void
  switchAccount: (uuid: string) => void

  versions: Version[]
  setVersions: (v: Version[]) => void

  selectedVersion: string
  setSelectedVersion: (v: string) => void

  ram: number
  setRam: (r: number) => void

  javaPath: string
  setJavaPath: (p: string) => void

  minecraftPath: string
  setMinecraftPath: (p: string) => void

  brightness: number
  setBrightness: (b: number) => void

  gameRunning: boolean
  setGameRunning: (r: boolean) => void
}

export const useStore = create<Store>()(
  persist(
    (set, get) => ({
      theme: 'chill',
      setTheme: (theme) => {
        set({ theme })
        document.documentElement.setAttribute('data-theme', theme)
      },
      toggleTheme: () => {
        const next = get().theme === 'chill' ? 'gamer' : 'chill'
        get().setTheme(next)
      },

      username: null,
      uuid: null,
      setUser: (username, uuid) => set({ username, uuid }),
      clearUser: () => set({ username: null, uuid: null }),

      accounts: [],
      addAccount: (username, uuid) => {
        const accounts = get().accounts
        const idx = accounts.findIndex((a) => a.uuid === uuid)
        let next: Account[]
        if (idx >= 0) {
          next = accounts.map((a, i) => (i === idx ? { username, uuid } : a))
        } else if (accounts.length < 2) {
          next = [...accounts, { username, uuid }]
        } else {
          next = accounts
        }
        set({ accounts: next, username, uuid })
      },
      removeAccount: (targetUuid) => {
        const accounts = get().accounts.filter((a) => a.uuid !== targetUuid)
        const activeUuid = get().uuid
        if (activeUuid === targetUuid) {
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

      versions: [],
      setVersions: (versions) => set({ versions }),

      selectedVersion: '',
      setSelectedVersion: (selectedVersion) => set({ selectedVersion }),

      ram: 4096,
      setRam: (ram) => set({ ram }),

      javaPath: '',
      setJavaPath: (javaPath) => set({ javaPath }),

      minecraftPath: '',
      setMinecraftPath: (minecraftPath) => set({ minecraftPath }),

      brightness: 100,
      setBrightness: (brightness) => set({ brightness }),

      gameRunning: false,
      setGameRunning: (gameRunning) => set({ gameRunning }),
    }),
    {
      name: 'yuyuframe-store',
      partialize: (s) => ({
        theme: s.theme,
        selectedVersion: s.selectedVersion,
        ram: s.ram,
        javaPath: s.javaPath,
        minecraftPath: s.minecraftPath,
        brightness: s.brightness,
        accounts: s.accounts,
        username: s.username,
        uuid: s.uuid,
      }),
    }
  )
)
