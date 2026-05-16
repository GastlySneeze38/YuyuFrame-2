interface Window {
  api?: {
    openExternal: (url: string) => Promise<void>
    minimize: () => void
    maximize: () => void
    close: () => void
  }
}
