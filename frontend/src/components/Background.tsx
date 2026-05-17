import { useMemo } from 'react'
import { useStore } from '@/stores/useStore'

// The 3 Chill palettes from the original ColorMonitor class
const PALETTES = [
  { s1: 'rgb(243,33,51)', s2: 'rgb(199,149,107)', s3: 'rgb(204,208,172)' }, // Morning
  { s1: 'rgb(2,193,64)',  s2: 'rgb(0,169,158)',   s3: 'rgb(1,133,207)'  }, // Afternoon
  { s1: 'rgb(2,100,64)',  s2: 'rgb(0,100,158)',   s3: 'rgb(1,50,207)'   }, // Night
]

function getPalette() {
  const h = new Date().getHours()
  if (h >= 6 && h < 13) return PALETTES[0]
  if (h >= 13 && h < 19) return PALETTES[1]
  return PALETTES[2]
}

// 25 cloud positions, fixed so they don't jump on re-render
const CLOUDS = [
  { x:5,  y:6,  w:60,  delay:0   }, { x:17, y:3,  w:45, delay:0.8 },
  { x:28, y:9,  w:75,  delay:1.6 }, { x:42, y:5,  w:38, delay:0.4 },
  { x:53, y:12, w:55,  delay:2.1 }, { x:65, y:4,  w:68, delay:1.2 },
  { x:76, y:8,  w:42,  delay:0.6 }, { x:87, y:3,  w:50, delay:1.9 },
  { x:92, y:14, w:35,  delay:0.3 }, { x:10, y:20, w:48, delay:1.5 },
  { x:33, y:24, w:32,  delay:2.3 }, { x:48, y:18, w:62, delay:0.9 },
  { x:60, y:26, w:40,  delay:1.7 }, { x:78, y:21, w:53, delay:0.1 },
  { x:3,  y:30, w:44,  delay:2.0 }, { x:22, y:34, w:37, delay:1.1 },
  { x:38, y:38, w:58,  delay:0.5 }, { x:55, y:32, w:46, delay:1.8 },
  { x:70, y:36, w:34,  delay:2.4 }, { x:85, y:28, w:64, delay:0.7 },
  { x:12, y:42, w:52,  delay:1.3 }, { x:30, y:44, w:39, delay:2.2 },
  { x:50, y:40, w:70,  delay:0.2 }, { x:68, y:44, w:43, delay:1.4 },
  { x:88, y:40, w:48,  delay:2.6 },
]

export function Background() {
  const { theme } = useStore()
  const isGamer = theme === 'gamer'
  const palette = useMemo(() => getPalette(), [])

  if (isGamer) {
    return (
      <div
        className="absolute inset-0"
        style={{ background: 'rgb(19,19,21)' }}
      >
        {/* Subtle purple center glow */}
        <div
          className="absolute inset-0"
          style={{
            background:
              'radial-gradient(ellipse at 50% 40%, rgb(68 62 185 / 0.08) 0%, transparent 65%)',
          }}
        />
      </div>
    )
  }

  // Chill: 3 horizontal bands + clouds + border dots
  const dotCount = 18

  return (
    <div className="absolute inset-0 overflow-hidden">
      {/* Band top 45% */}
      <div className="absolute left-0 right-0 top-0" style={{ height: '45%', background: palette.s1 }} />
      {/* Band middle 5% */}
      <div className="absolute left-0 right-0" style={{ top: '45%', height: '5%', background: palette.s2 }} />
      {/* Band bottom 50% */}
      <div className="absolute left-0 right-0 bottom-0" style={{ top: '50%', background: palette.s3 }} />

      {/* Clouds */}
      {CLOUDS.map((c, i) => (
        <div
          key={i}
          className={i % 2 === 0 ? 'animate-float' : 'animate-float-slow'}
          style={{
            position: 'absolute',
            left: `${c.x}%`,
            top: `${c.y}%`,
            width: c.w,
            height: Math.round(c.w * 0.45),
            animationDelay: `${c.delay}s`,
          }}
        >
          <div
            className="h-full w-full rounded-full"
            style={{ background: 'rgba(255,255,255,0.7)', filter: 'blur(7px)' }}
          />
        </div>
      ))}

      {/* Left border dots */}
      {Array.from({ length: dotCount }, (_, i) => (
        <div
          key={`l${i}`}
          className="absolute"
          style={{
            left: 6,
            top: `${5 + i * (90 / dotCount)}%`,
            width: 4,
            height: 4,
            borderRadius: '50%',
            background: 'rgba(0,0,0,0.55)',
          }}
        />
      ))}

      {/* Right border dots */}
      {Array.from({ length: dotCount }, (_, i) => (
        <div
          key={`r${i}`}
          className="absolute"
          style={{
            right: 6,
            top: `${5 + i * (90 / dotCount)}%`,
            width: 4,
            height: 4,
            borderRadius: '50%',
            background: 'rgba(0,0,0,0.55)',
          }}
        />
      ))}
    </div>
  )
}

// Export palette so the controls panel can match its background
export function useChillPalette() {
  return useMemo(() => getPalette(), [])
}
