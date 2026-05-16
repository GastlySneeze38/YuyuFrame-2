import type {
  AuthStatus,
  DeviceAuthResponse,
  PollResponse,
  ProgressResponse,
  Version,
} from '@/types'

const BASE = 'http://127.0.0.1:3847'

async function get<T>(path: string): Promise<T> {
  const res = await fetch(`${BASE}${path}`)
  if (!res.ok) throw new Error(`GET ${path} failed: ${res.status}`)
  return res.json()
}

async function post<T>(path: string, body?: unknown): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    method: 'POST',
    headers: body ? { 'Content-Type': 'application/json' } : {},
    body: body ? JSON.stringify(body) : undefined,
  })
  if (!res.ok) throw new Error(`POST ${path} failed: ${res.status}`)
  return res.json()
}

export const api = {
  health: () => get<{ status: string }>('/api/health'),

  versions: {
    list: () => get<Version[]>('/api/versions'),
  },

  auth: {
    status: () => get<AuthStatus>('/api/auth/status'),
    startDevice: () => post<DeviceAuthResponse>('/api/auth/device'),
    poll: () => get<PollResponse>('/api/auth/poll'),
    logout: () => post<void>('/api/auth/logout'),
  },

  launch: {
    start: (version: string, ram?: number) =>
      post<{ success: boolean; message: string }>('/api/launch', { version, ram }),
    progress: () => get<ProgressResponse>('/api/launch/progress'),
  },
}
