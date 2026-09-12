/**
 * PetPomodoro — Engine: reducer thuần cho các sự kiện core (spec §1.1).
 * Mọi mutation gameplay đi qua reduce()/applyElapsedDays() — không có lối tắt.
 * Engine KHÔNG biết gì về hệ cụ thể: chỉ dispatch event, các hệ tự xử.
 */
import type { DayRecord, ElementId, GameEvent, GameState } from './types'
import {
  COIN_PER_FEED,
  GOAL_SESSIONS,
  defaultSettings,
  nextDayKey,
  statusBand,
} from './formulas'
import { dispatch, runXpGain } from './systems'
import { lineById } from './catalog'

export const HISTORY_CAP = 30

export interface ReduceResult {
  state: GameState
  events: GameEvent[]
  /** feed bị từ chối (hết sushi) → state giữ nguyên. */
  rejected: boolean
}

/** State mới cho pet vừa nở (onboarding "Nhận … về nhà"). */
export function newGame(speciesId: ElementId, today: string): GameState {
  const line = lineById(speciesId)
  return {
    pet: {
      speciesId,
      stageId: line.stages[0].id,
      level: 1,
      xp: 0,
      condition: 80, // Khỏe mạnh
      coins: 12,
      streak: 0,
      todaySessions: 0,
      todayMinutes: 0,
      hatchedAt: new Date().toISOString(),
    },
    meta: {
      today,
      lastActiveDate: today,
      timer: null,
      focusStreak: 0,
      settings: defaultSettings(),
      alerts: { sound: true, notify: true },
    },
    systems: {
      power: { value: 40 },
      evolution: { unlocked: [line.stages[0].id], branchId: null },
      stats: {
        todayXp: 0,
        todayFed: 0,
        lastSessionDate: null,
        healthyDays: 0,
        history: [],
        totalSessions: 0,
        totalMinutes: 0,
        totalXp: 0,
        totalFeeds: 0,
      },
    },
  }
}

function clone(s: GameState): GameState {
  return structuredClone(s)
}

function touch(s: GameState): void {
  s.meta.lastActiveDate = s.meta.today
}

/** Kết thúc ngày D: decay/power/streak/history qua hệ thống, rồi reset bộ đếm ngày. */
function closeDay(s: GameState, out: GameEvent[]): void {
  const evt: GameEvent = {
    type: 'day_end',
    date: s.meta.today,
    sessions: s.pet.todaySessions,
    fed: s.systems.stats.todayFed,
  }
  out.push(...dispatch(s, evt))

  const record: DayRecord = {
    date: s.meta.today,
    sessions: s.pet.todaySessions,
    minutes: s.pet.todayMinutes,
    xp: s.systems.stats.todayXp,
    goal: s.pet.todaySessions >= GOAL_SESSIONS,
  }
  s.systems.stats.history.push(record)
  if (s.systems.stats.history.length > HISTORY_CAP) {
    s.systems.stats.history.splice(0, s.systems.stats.history.length - HISTORY_CAP)
  }
  s.systems.stats.healthyDays =
    statusBand(s.pet.condition) === 'khoemanh' ? s.systems.stats.healthyDays + 1 : 0

  s.meta.today = nextDayKey(s.meta.today)
  s.pet.todaySessions = 0
  s.pet.todayMinutes = 0
  s.systems.stats.todayXp = 0
  s.systems.stats.todayFed = 0
}

/**
 * Real decay khi mở app: kéo state "hôm nay" bám theo ngày thật.
 * Mỗi ngày lùi lại: power (−6 trống / −2 đủ mục tiêu / 0 một phần),
 * condition −10 nếu ngày không ăn & không phiên, streak đứt nếu ngày không phiên.
 */
export function applyElapsedDays(state: GameState, today: string): ReduceResult {
  if (state.meta.today >= today) return { state, events: [], rejected: false }
  const s = clone(state)
  const events: GameEvent[] = []
  let guard = 0
  while (s.meta.today < today && guard++ < 400) closeDay(s, events)
  touch(s)
  return { state: s, events, rejected: false }
}

/** Reducer cho các sự kiện core. Thuần: không side-effect, trả state mới. */
export function reduce(state: GameState, event: GameEvent): ReduceResult {
  switch (event.type) {
    case 'session_complete': {
      const s = clone(state)
      s.pet.todaySessions += 1
      s.pet.todayMinutes += event.minutes
      s.systems.stats.totalSessions += 1
      s.systems.stats.totalMinutes += event.minutes
      const events = dispatch(s, event)
      touch(s)
      return { state: s, events, rejected: false }
    }
    case 'feed': {
      if (state.pet.coins < COIN_PER_FEED) return { state, events: [], rejected: true }
      const s = clone(state)
      const events = dispatch(s, event)
      touch(s)
      return { state: s, events, rejected: false }
    }
    case 'day_end': {
      const s = clone(state)
      const events = dispatch(s, event)
      return { state: s, events, rejected: false }
    }
    default:
      return { state, events: [], rejected: false }
  }
}

/** DEV-only: hoàn thành phiên ngay (không đụng timer thật). */
export function devCompleteSession(state: GameState): ReduceResult {
  return reduce(state, {
    type: 'session_complete',
    minutes: state.meta.settings.focusMinutes,
    source: 'dev',
  })
}

/** DEV-only: cộng XP để thử mốc tiến hoá Lv.8/Lv.16. */
export function devGrantXp(state: GameState, amount: number): ReduceResult {
  const s = clone(state)
  const events = runXpGain(s, amount)
  touch(s)
  return { state: s, events, rejected: false }
}

/** DEV-only: mô phỏng "qua ngày mới" (kết thúc ngày hiện tại của state). */
export function devNextDay(state: GameState): ReduceResult {
  const s = clone(state)
  const events: GameEvent[] = []
  closeDay(s, events)
  touch(s)
  return { state: s, events, rejected: false }
}
