/**
 * PetPomodoro — Công thức số CHUNG (spec §3, không đổi giữa các kịch bản).
 */
import type { StatusBand, TimerSettings } from './types'

export const XP_PER_FOCUS_MINUTE = 1
export const XP_PER_FEED = 5
export const POWER_PER_SESSION = 8
export const COIN_PER_SESSION = 1
export const COIN_PER_FEED = 1
export const CONDITION_PER_FEED = 15
export const GOAL_SESSIONS = 3 // mục tiêu ngày: ≥ 3 phiên focus

/** Decay spec §3: ngày không phiên −6 · ngày đủ mục tiêu −2 (partial: 0 — tự quy ước). */
export const DECAY_POWER_IDLE = -6
export const DECAY_POWER_GOAL = -2
/** Thể trạng: −10 mỗi ngày không ăn & không phiên. */
export const DECAY_CONDITION_IDLE = -10

/* ---------- Cài đặt pomodoro (user chỉnh được — doc `state/settings`) ---------- */
export const DEFAULT_FOCUS_MINUTES = 25
export const DEFAULT_BREAK_MINUTES = 5
export const DEFAULT_LONG_BREAK_MINUTES = 15
export const DEFAULT_SESSIONS_BEFORE_LONG_BREAK = 4

/** Giới hạn từng cài đặt (min/max/bước nhảy ở màn Cài đặt) — chặn giá trị vô lý. */
export const SETTING_LIMITS = {
  focusMinutes: { min: 5, max: 120, step: 5 },
  breakMinutes: { min: 1, max: 30, step: 1 },
  longBreakMinutes: { min: 5, max: 60, step: 5 },
  sessionsBeforeLongBreak: { min: 2, max: 8, step: 1 },
} as const

export type SettingKey = keyof typeof SETTING_LIMITS

export function defaultSettings(): TimerSettings {
  return {
    focusMinutes: DEFAULT_FOCUS_MINUTES,
    breakMinutes: DEFAULT_BREAK_MINUTES,
    longBreakMinutes: DEFAULT_LONG_BREAK_MINUTES,
    sessionsBeforeLongBreak: DEFAULT_SESSIONS_BEFORE_LONG_BREAK,
  }
}

/** Chuẩn hoá 1 giá trị cài đặt về đúng [min, max] — dùng cho cả UI lẫn hydrate storage. */
export function clampSetting(key: SettingKey, value: number): number {
  const { min, max } = SETTING_LIMITS[key]
  return clamp(Math.round(value), min, max)
}

/** Docs/storage → TimerSettings hợp lệ (thiếu mục nào lấy mặc định mục đó). */
export function clampSettings(raw: Record<string, unknown>): TimerSettings {
  const base = defaultSettings()
  return {
    focusMinutes: clampSetting('focusMinutes', num(raw.focusMinutes, base.focusMinutes)),
    breakMinutes: clampSetting('breakMinutes', num(raw.breakMinutes, base.breakMinutes)),
    longBreakMinutes: clampSetting('longBreakMinutes', num(raw.longBreakMinutes, base.longBreakMinutes)),
    sessionsBeforeLongBreak: clampSetting(
      'sessionsBeforeLongBreak',
      num(raw.sessionsBeforeLongBreak, base.sessionsBeforeLongBreak),
    ),
  }
}

export const focusMsOf = (s: TimerSettings): number => s.focusMinutes * 60_000
export const breakMsOf = (s: TimerSettings, long: boolean): number =>
  (long ? s.longBreakMinutes : s.breakMinutes) * 60_000

/** Đã đủ chuỗi phiên → lần nghỉ kế tiếp là NGHỈ DÀI. */
export function isLongBreakDue(settings: TimerSettings, focusStreak: number): boolean {
  const n = settings.sessionsBeforeLongBreak
  return n > 0 && focusStreak > 0 && focusStreak % n === 0
}

/** XP lên mức: need(L) = 80 + 30×L → Lv.4→5 cần 200 XP. */
export function xpNeed(level: number): number {
  return 80 + 30 * level
}

/** Cap Sức mạnh theo level: cap(L) = min(100, 40 + 6×L). */
export function powerCap(level: number): number {
  return Math.min(100, 40 + 6 * level)
}

export function statusBand(condition: number): StatusBand {
  if (condition >= 70) return 'khoemanh'
  if (condition >= 30) return 'binhthuong'
  return 'omyeu'
}

export const STATUS_LABEL: Record<StatusBand, string> = {
  khoemanh: 'Khỏe mạnh',
  binhthuong: 'Bình thường',
  omyeu: 'Ốm yếu',
}

export function clamp(n: number, min: number, max: number): number {
  return Math.max(min, Math.min(max, n))
}

/** Số an toàn: không phải số hữu hạn → trả fallback (hydrate storage bẩn). */
function num(v: unknown, d: number): number {
  return typeof v === 'number' && Number.isFinite(v) ? v : d
}

/* ---------- Ngày (YYYY-MM-DD, giờ local) ---------- */

export function toDateKey(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

export function todayKey(): string {
  return toDateKey(new Date())
}

function keyToUtc(key: string): number {
  const [y = 1970, m = 1, d = 1] = key.split('-').map(Number)
  return Date.UTC(y, m - 1, d)
}

function utcToKey(ms: number): string {
  const d = new Date(ms)
  const y = d.getUTCFullYear()
  const m = String(d.getUTCMonth() + 1).padStart(2, '0')
  const day = String(d.getUTCDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

export function addDaysKey(key: string, n: number): string {
  return utcToKey(keyToUtc(key) + n * 86_400_000)
}

export const nextDayKey = (key: string): string => addDaysKey(key, 1)
export const prevDayKey = (key: string): string => addDaysKey(key, -1)

/** Số ngày b=a-b (theo lịch, không quan tâm giờ). */
export function daysBetween(a: string, b: string): number {
  return Math.round((keyToUtc(a) - keyToUtc(b)) / 86_400_000)
}
