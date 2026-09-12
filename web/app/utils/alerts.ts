/**
 * alerts — âm báo tổng hợp (Web Audio API, không cần file) + thông báo hệ thống.
 * Âm chỉ phát được sau khi AudioContext được "mở khóa" bằng một tương tác
 * (unlockAudio trong pointerdown/click — chính sách autoplay của trình duyệt).
 * Thông báo chỉ bắn khi tab đang ẩn (document.hidden), tránh trùng toast trên trang.
 */

export type ChimeKind = 'focusDone' | 'breakDone' | 'feed' | 'evolve'

let audioCtx: AudioContext | null = null

function getCtx(): AudioContext | null {
  if (typeof window === 'undefined') return null
  try {
    if (!audioCtx) {
      const AC =
        window.AudioContext ??
        (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext
      if (!AC) return null
      audioCtx = new AC()
    }
    return audioCtx
  } catch {
    return null
  }
}

/** Gọi trong user-gesture (click Bắt đầu / toggle cài đặt) để mở khóa âm thanh. */
export function unlockAudio(): void {
  const c = getCtx()
  if (c && c.state === 'suspended') void c.resume().catch(() => {})
}

/** 1 nốt: sine + envelope mềm, volume nhỏ (~8%) cho đỡ giật mình. */
function tone(c: AudioContext, freq: number, start: number, dur: number, peak = 0.08): void {
  const osc = c.createOscillator()
  const gain = c.createGain()
  osc.type = 'sine'
  osc.frequency.value = freq
  const t0 = c.currentTime + start
  gain.gain.setValueAtTime(0.0001, t0)
  gain.gain.linearRampToValueAtTime(peak, t0 + 0.015)
  gain.gain.exponentialRampToValueAtTime(0.0001, t0 + dur)
  osc.connect(gain)
  gain.connect(c.destination)
  osc.start(t0)
  osc.stop(t0 + dur + 0.05)
}

const MELODIES: Record<ChimeKind, { freq: number; at: number; dur: number; peak?: number }[]> = {
  focusDone: [
    { freq: 523.25, at: 0, dur: 0.28 },
    { freq: 659.25, at: 0.13, dur: 0.28 },
    { freq: 783.99, at: 0.26, dur: 0.42 },
  ],
  breakDone: [
    { freq: 392.0, at: 0, dur: 0.26 },
    { freq: 523.25, at: 0.14, dur: 0.4 },
  ],
  feed: [{ freq: 987.77, at: 0, dur: 0.16, peak: 0.06 }],
  evolve: [
    { freq: 523.25, at: 0, dur: 0.22 },
    { freq: 659.25, at: 0.12, dur: 0.22 },
    { freq: 783.99, at: 0.24, dur: 0.22 },
    { freq: 1046.5, at: 0.36, dur: 0.55 },
  ],
}

export function playChime(kind: ChimeKind): void {
  try {
    const c = getCtx()
    if (!c) return
    if (c.state === 'suspended') void c.resume().catch(() => {})
    for (const n of MELODIES[kind]) tone(c, n.freq, n.at, n.dur, n.peak ?? 0.08)
  } catch {
    /* âm báo là phụ — không để vỡ app */
  }
}

/** Xin quyền thông báo — gọi trong user-gesture (click Bắt đầu phiên / toggle Cài đặt). */
export async function ensureNotifyPermission(): Promise<void> {
  try {
    if (typeof window === 'undefined' || !('Notification' in window)) return
    if (Notification.permission === 'default') await Notification.requestPermission()
  } catch {
    /* ignore */
  }
}

/**
 * Thông báo chỉ khi tab đang ẩn. Ưu tiên đường service worker (bắt buộc cho
 * PWA trên Android Chrome), fallback `new Notification` (desktop) với
 * click → quay lại tab. Click trên đường SW sẽ không focus về app — hạn chế đã biết.
 */
export async function notifyIfHidden(title: string, body: string): Promise<void> {
  try {
    if (typeof window === 'undefined' || !document.hidden) return
    if (!('Notification' in window) || Notification.permission !== 'granted') return
    const opts: NotificationOptions = {
      body,
      icon: '/icon-192.png',
      badge: '/icon-192.png',
      tag: 'petpomodoro-timer',
    }
    const reg = await navigator.serviceWorker?.getRegistration()
    if (reg) {
      await reg.showNotification(title, opts)
      return
    }
    const n = new Notification(title, opts)
    n.onclick = () => {
      window.focus()
      n.close()
    }
  } catch {
    /* ignore */
  }
}
