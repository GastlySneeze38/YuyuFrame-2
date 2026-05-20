import { invoke } from '@tauri-apps/api/core'
import type { AuthStatus, DeviceAuthResponse, Mod, PollResponse, Version } from '@/types'

// ── Types ────────────────────────────────────────────────────────────────────

export interface YuyuStatusResp {
  has_account: boolean
}

export interface YuyuLoginResp {
  token: string
  username: string
  accounts: McAccountInfo[]
}

export interface McAccountInfo {
  mc_username: string
  mc_uuid: string
  is_active: boolean
}

// ── API ──────────────────────────────────────────────────────────────────────

export const api = {
  versions: {
    list: () => invoke<Version[]>('list_versions'),
  },

  yuyu: {
    status: () => invoke<YuyuStatusResp>('yuyu_status'),
    register: (username: string, password: string) =>
      invoke<YuyuLoginResp>('yuyu_register', { username, password }),
    login: (username: string, password: string) =>
      invoke<YuyuLoginResp>('yuyu_login', { username, password }),
    logout: () => invoke<void>('yuyu_logout'),
  },

  auth: {
    status: () => invoke<AuthStatus>('auth_status'),
    startDevice: () => invoke<DeviceAuthResponse>('auth_start_device'),
    poll: () => invoke<PollResponse>('auth_poll'),
    logout: () => invoke<void>('auth_logout'),
  },

  mc: {
    accounts: () => invoke<McAccountInfo[]>('mc_list_accounts'),
    switch: (uuid: string) => invoke<McAccountInfo>('mc_switch', { uuid }),
    delete: (uuid: string) => invoke<void>('mc_delete', { uuid }),
  },

  launch: {
    start: (version: string, ram?: number, loader?: string) =>
      invoke<void>('launch_game', { version, ram, loader }),
  },

  mods: {
    list: () => invoke<Mod[]>('mods_list'),
    toggle: (name: string) => invoke<Mod>('mods_toggle', { name }),
    delete: (name: string) => invoke<void>('mods_delete', { name }),
    install: (url: string, filename: string) =>
      invoke<Mod>('mods_install', { url, filename }),

    upload: async (file: File): Promise<Mod> => {
      const data = Array.from(new Uint8Array(await file.arrayBuffer()))
      return invoke<Mod>('mods_upload', { filename: file.name, data })
    },
  },
}
