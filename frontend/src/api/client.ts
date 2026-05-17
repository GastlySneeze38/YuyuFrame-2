import type {
  AuthStatus,
  DeviceAuthResponse,
  Mod,
  PollResponse,
  ProgressResponse,
  Version,
} from '@/types'

const BASE = 'http://127.0.0.1:3847'

// Module-level token — set after yuyu login, cleared on logout
let _yuyuToken: string | null = null

export function setApiToken(token: string | null) {
  _yuyuToken = token
}

function authHeaders(withJson = false): Record<string, string> {
  const h: Record<string, string> = {}
  if (withJson) h['Content-Type'] = 'application/json'
  if (_yuyuToken) h['X-Yuyu-Token'] = _yuyuToken
  return h
}

async function get<T>(path: string): Promise<T> {
  const res = await fetch(`${BASE}${path}`, { headers: authHeaders() })
  if (!res.ok) throw new Error(`GET ${path} échoué: ${res.status}`)
  return res.json()
}

async function post<T>(path: string, body?: unknown): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    method: 'POST',
    headers: authHeaders(!!body),
    body: body ? JSON.stringify(body) : undefined,
  })
  if (!res.ok) {
    const msg = await res.text().catch(() => String(res.status))
    throw new Error(msg || `POST ${path} échoué: ${res.status}`)
  }
  if (res.status === 204 || res.headers.get('content-length') === '0') return undefined as T
  return res.json()
}

async function del<T>(path: string): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    method: 'DELETE',
    headers: authHeaders(),
  })
  if (!res.ok) throw new Error(`DELETE ${path} échoué: ${res.status}`)
  return undefined as T
}

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
  health: () => get<{ status: string }>('/api/health'),

  versions: {
    list: () => get<Version[]>('/api/versions'),
  },

  yuyu: {
    status: () => get<YuyuStatusResp>('/api/yuyu/status'),
    register: (username: string, password: string) =>
      post<YuyuLoginResp>('/api/yuyu/register', { username, password }),
    login: (username: string, password: string) =>
      post<YuyuLoginResp>('/api/yuyu/login', { username, password }),
    logout: () => post<void>('/api/yuyu/logout'),
  },

  auth: {
    status: () => get<AuthStatus>('/api/auth/status'),
    startDevice: () => post<DeviceAuthResponse>('/api/auth/device'),
    poll: () => get<PollResponse>('/api/auth/poll'),
    logout: () => post<void>('/api/auth/logout'),
  },

  mc: {
    accounts: () => get<McAccountInfo[]>('/api/mc/accounts'),
    switch: (uuid: string) => post<McAccountInfo>('/api/mc/switch', { uuid }),
    delete: (uuid: string) => del<void>(`/api/mc/account/${uuid}`),
  },

  launch: {
    start: (version: string, ram?: number, loader?: string) =>
      post<{ success: boolean; message: string }>('/api/launch', { version, ram, loader }),
    progress: () => get<ProgressResponse>('/api/launch/progress'),
  },

  mods: {
    list: () => get<Mod[]>('/api/mods'),
    toggle: async (name: string): Promise<Mod> => {
      const res = await fetch(`${BASE}/api/mods/${encodeURIComponent(name)}/toggle`, {
        method: 'PUT',
        headers: authHeaders(),
      })
      return res.json()
    },
    delete: (name: string) => del<void>(`/api/mods/${encodeURIComponent(name)}`),
    upload: async (file: File): Promise<Mod> => {
      const form = new FormData()
      form.append('file', file)
      const res = await fetch(`${BASE}/api/mods/upload`, {
        method: 'POST',
        headers: authHeaders(),
        body: form,
      })
      if (!res.ok) {
        const msg = await res.text().catch(() => String(res.status))
        throw new Error(msg)
      }
      return res.json()
    },
  },
}
