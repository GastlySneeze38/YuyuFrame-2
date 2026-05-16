import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { Theme, Version } from '@/types'

interface Store {
  theme: Theme
  setTheme: (t: Theme) => void
  toggleTheme: () => void

  username: string | null
  uuid: string | null
  setUser: (username: string, uuid: string) => void
  clearUser: () => void

  versions: Version[]
  setVersions: (v: Version[]) => void

  selectedVersion: string
  setSelectedVersion: (v: string) => void

  ram: number
  setRam: (r: number) => void

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

      versions: [],
      setVersions: (versions) => set({ versions }),

      selectedVersion: '',
      setSelectedVersion: (selectedVersion) => set({ selectedVersion }),

      ram: 4096,
      setRam: (ram) => set({ ram }),

      gameRunning: false,
      setGameRunning: (gameRunning) => set({ gameRunning }),
    }),
    {
      name: 'yuyuframe-store',
      partialize: (s) => ({
        theme: s.theme,
        selectedVersion: s.selectedVersion,
        ram: s.ram,
      }),
    }
  )
)
