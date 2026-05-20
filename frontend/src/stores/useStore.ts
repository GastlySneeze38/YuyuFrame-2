import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { Instance, Theme, Version, Account } from '@/types'

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

  // ── Minecraft account list ─────────────────────────────────────────────────
  accounts: Account[]
  setAccounts: (accounts: Account[]) => void
  addAccount: (username: string, uuid: string) => void
  removeAccount: (uuid: string) => void
  switchAccount: (uuid: string) => void

  // ── Versions (for instance creation) ──────────────────────────────────────
  versions: Version[]
  setVersions: (v: Version[]) => void

  // ── Instances ─────────────────────────────────────────────────────────────
  instances: Instance[]
  setInstances: (instances: Instance[]) => void
  addInstance: (instance: Instance) => void
  updateInstance: (instance: Instance) => void
  removeInstance: (id: string) => void

  selectedInstanceId: string | null
  setSelectedInstanceId: (id: string | null) => void
  selectedInstance: () => Instance | null

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

  // ── Last session ───────────────────────────────────────────────────────────
  lastSession: { instanceName: string; at: string } | null
  setLastSession: (s: { instanceName: string; at: string }) => void
}

export const useStore = create<Store>()(
  persist(
    (set, get) => ({
      // YuyuFrame session
      yuyuToken: null,
      yuyuUsername: null,
      setYuyuSession: (token, username) => set({ yuyuToken: token, yuyuUsername: username }),
      clearYuyuSession: () =>
        set({ yuyuToken: null, yuyuUsername: null, accounts: [], username: null, uuid: null }),

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

      // Instances
      instances: [],
      setInstances: (instances) => set({ instances }),
      addInstance: (instance) => set((s) => ({ instances: [...s.instances, instance] })),
      updateInstance: (instance) =>
        set((s) => ({
          instances: s.instances.map((i) => (i.id === instance.id ? instance : i)),
        })),
      removeInstance: (id) =>
        set((s) => ({
          instances: s.instances.filter((i) => i.id !== id),
          selectedInstanceId: s.selectedInstanceId === id ? null : s.selectedInstanceId,
        })),

      selectedInstanceId: null,
      setSelectedInstanceId: (id) => set({ selectedInstanceId: id }),
      selectedInstance: () => {
        const { instances, selectedInstanceId } = get()
        return instances.find((i) => i.id === selectedInstanceId) ?? null
      },

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

      // Last session
      lastSession: null,
      setLastSession: (lastSession) => set({ lastSession }),
    }),
    {
      name: 'yuyuframe-store',
      partialize: (s) => ({
        theme: s.theme,
        selectedInstanceId: s.selectedInstanceId,
        ram: s.ram,
        javaPath: s.javaPath,
        minecraftPath: s.minecraftPath,
        brightness: s.brightness,
        username: s.username,
        uuid: s.uuid,
        lastSession: s.lastSession,
      }),
    }
  )
)
