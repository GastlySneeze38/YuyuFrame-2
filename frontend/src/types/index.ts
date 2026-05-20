export interface Version {
  id: string
  version_type: 'release' | 'snapshot'
  url: string
}

export interface AuthStatus {
  authenticated: boolean
  username: string | null
  uuid: string | null
}

export interface DeviceAuthResponse {
  user_code: string
  verification_uri: string
  expires_in: number
}

export interface PollResponse {
  status: 'pending' | 'success' | 'error'
  username: string | null
  error: string | null
}

export interface ProgressResponse {
  downloading: boolean
  current: number
  total: number
  message: string
  percent: number
}

export interface Mod {
  name: string
  size: number
  enabled: boolean
}

export type Loader = 'vanilla' | 'fabric' | 'forge'

export interface Instance {
  id: string
  name: string
  mc_version: string
  loader: Loader
  ram_mb: number
}

export type Theme = 'chill' | 'gamer'

export interface Account {
  username: string
  uuid: string
}
