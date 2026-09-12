/**
 * PetPomodoro — Persistence (spec §1.5).
 * Key docs mirror Firestore: `state/pet` (core) + `state/meta` + `systems/{id}`.
 * M2 đổi sang Firebase = viết FirebaseAdapter cùng interface, KHÔNG đụng engine/UI.
 * Thêm hệ mới = thêm 1 doc `systems/{id}` — doc hệ cũ không bị đụng tới.
 */
import type { ElementId, GameState, PetCore } from './types'
import { lineById, stageById } from './catalog'
import { newGame } from './engine'
import { clampSettings, todayKey } from './formulas'

export type DocPath =
  | 'state/pet'
  | 'state/meta'
  | 'state/settings'
  | 'systems/power'
  | 'systems/evolution'
  | 'systems/stats'
export type DocData = Record<string, unknown>
export type SerializedDocs = Partial<Record<DocPath, DocData>>

export interface PersistenceAdapter {
  loadDocs(): Promise<SerializedDocs | null>
  saveDocs(docs: SerializedDocs): Promise<void>
  deleteAll(): Promise<void>
}

export function stateToDocs(s: GameState): SerializedDocs {
  const { settings, ...meta } = s.meta
  return {
    'state/pet': { ...s.pet } as unknown as DocData,
    'state/meta': { ...meta, timer: s.meta.timer ? { ...s.meta.timer } : null },
    'state/settings': { ...settings },
    'systems/power': { ...s.systems.power },
    'systems/evolution': {
      ...s.systems.evolution,
      unlocked: [...s.systems.evolution.unlocked],
    },
    'systems/stats': {
      ...s.systems.stats,
      history: s.systems.stats.history.map((h) => ({ ...h })),
    },
  }
}

const num = (v: unknown, d: number): number => (typeof v === 'number' && Number.isFinite(v) ? v : d)
const str = (v: unknown, d: string): string => (typeof v === 'string' ? v : d)
const bool = (v: unknown, d: boolean): boolean => (typeof v === 'boolean' ? v : d)

function hydratePet(raw: DocData | undefined): PetCore | null {
  if (!raw) return null
  const speciesId = str(raw.speciesId, '')
  if (!isElement(speciesId)) return null
  let line
  try {
    line = lineById(speciesId)
  } catch {
    return null
  }
  const stageId = str(raw.stageId, line.stages[0].id)
  return {
    speciesId,
    stageId: stageById(line, stageId) ? stageId : line.stages[0].id,
    level: Math.max(1, num(raw.level, 1)),
    xp: Math.max(0, num(raw.xp, 0)),
    condition: Math.min(100, Math.max(0, num(raw.condition, 80))),
    coins: Math.max(0, num(raw.coins, 0)),
    streak: Math.max(0, num(raw.streak, 0)),
    todaySessions: Math.max(0, num(raw.todaySessions, 0)),
    todayMinutes: Math.max(0, num(raw.todayMinutes, 0)),
    hatchedAt: str(raw.hatchedAt, new Date().toISOString()),
  }
}

function isElement(v: string): v is ElementId {
  return v === 'lua' || v === 'thuy' || v === 'thao'
}

/** Docs → GameState. Thiếu doc hệ nào thì khôi phục default của hệ đó (thêm hệ không vỡ state cũ). */
export function docsToState(docs: SerializedDocs): GameState | null {
  const pet = hydratePet(docs['state/pet'])
  if (!pet) return null
  const base = newGame(pet.speciesId, todayKey())
  const meta = docs['state/meta'] ?? {}
  const power = docs['systems/power'] ?? {}
  const evo = docs['systems/evolution'] ?? {}
  const stats = docs['systems/stats'] ?? {}

  const timerRaw = meta.timer as Record<string, unknown> | null | undefined
  const phase: 'focus' | 'break' | null =
    timerRaw?.phase === 'break' ? 'break' : timerRaw?.phase === 'focus' ? 'focus' : null
  const remainingMs = Math.max(0, num(timerRaw?.remainingMs, 0))
  // totalMs thiếu (save cũ) → lùi về remainingMs để progress/XP không vỡ.
  const totalMs = Math.max(remainingMs, num(timerRaw?.totalMs, remainingMs))
  const timer = phase
    ? {
        phase,
        running: bool(timerRaw?.running, false),
        remainingMs,
        endAt: typeof timerRaw?.endAt === 'number' ? timerRaw.endAt : null,
        totalMs,
      }
    : null

  // Cài đặt pomodoro: save cũ chưa có doc → tự lấy mặc định (thêm hệ không vỡ state cũ).
  const settingsRaw = (docs['state/settings'] ?? {}) as DocData
  // Tuỳ chọn cảnh báo: save cũ chưa có → mặc định bật cả hai.
  const alertsRaw = (meta.alerts ?? {}) as Record<string, unknown>

  return {
    pet,
    meta: {
      today: str(meta.today, base.meta.today),
      lastActiveDate: str(meta.lastActiveDate, str(meta.today, base.meta.today)),
      timer,
      focusStreak: Math.max(0, num(meta.focusStreak, 0)),
      settings: clampSettings(settingsRaw),
      alerts: { sound: bool(alertsRaw.sound, true), notify: bool(alertsRaw.notify, true) },
    },
    systems: {
      power: { value: Math.min(100, Math.max(0, num(power.value, base.systems.power.value))) },
      evolution: {
        unlocked: Array.isArray(evo.unlocked)
          ? (evo.unlocked as string[]).filter((x) => typeof x === 'string')
          : base.systems.evolution.unlocked,
        branchId:
          evo.branchId === 'hien' || evo.branchId === 'chien-binh' ? evo.branchId : null,
      },
      stats: {
        todayXp: Math.max(0, num(stats.todayXp, 0)),
        todayFed: Math.max(0, num(stats.todayFed, 0)),
        lastSessionDate: typeof stats.lastSessionDate === 'string' ? stats.lastSessionDate : null,
        healthyDays: Math.max(0, num(stats.healthyDays, 0)),
        history: Array.isArray(stats.history)
          ? (stats.history as unknown[]).filter(isDayRecord)
          : [],
        totalSessions: Math.max(0, num(stats.totalSessions, 0)),
        totalMinutes: Math.max(0, num(stats.totalMinutes, 0)),
        totalXp: Math.max(0, num(stats.totalXp, 0)),
        totalFeeds: Math.max(0, num(stats.totalFeeds, 0)),
      },
    },
  }
}

function isDayRecord(r: unknown): r is import('./types').DayRecord {
  if (typeof r !== 'object' || r === null) return false
  const o = r as Record<string, unknown>
  return typeof o.date === 'string' && typeof o.sessions === 'number'
}

/**
 * LocalStorageAdapter — lưu MỖI DOC theo đúng key Firestore-shape trong 1 JSON map,
 * saveDoc/setDoc semantics theo từng key → M2 map thẳng sang Firestore.
 */
export class LocalStorageAdapter implements PersistenceAdapter {
  constructor(private storageKey = 'petpomodoro.m1.docs') {}

  private readAll(): Record<string, DocData> {
    if (typeof localStorage === 'undefined') return {} // SSR / node
    try {
      const raw = localStorage.getItem(this.storageKey)
      if (!raw) return {}
      const parsed = JSON.parse(raw) as Record<string, unknown>
      return typeof parsed === 'object' && parsed !== null ? (parsed as Record<string, DocData>) : {}
    } catch {
      return {}
    }
  }

  async loadDocs(): Promise<SerializedDocs | null> {
    const all = this.readAll()
    if (!all['state/pet']) return null
    return all as SerializedDocs
  }

  async saveDocs(docs: SerializedDocs): Promise<void> {
    if (typeof localStorage === 'undefined') return // SSR / node
    const all = this.readAll()
    for (const [path, data] of Object.entries(docs)) {
      if (data === undefined) continue
      all[path] = data
    }
    try {
      localStorage.setItem(this.storageKey, JSON.stringify(all))
    } catch {
      /* storage đầy/bị chặn — app vẫn chạy trong phiên */
    }
  }

  async deleteAll(): Promise<void> {
    if (typeof localStorage === 'undefined') return // SSR / node
    try {
      localStorage.removeItem(this.storageKey)
    } catch {
      /* ignore */
    }
  }
}
