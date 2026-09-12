/**
 * PetPomodoro — Registry điều kiện dùng chung (spec §1.3, combo AND/OR).
 * Thêm loại mới = đăng ký 1 key, KHÔNG sửa hệ cũ.
 * Ví dụ mở rộng (spec §1.6):
 *   COND.total_sessions_at_least = (s, c) => s.totalSessions >= (c.value ?? 0)
 */
import type { GameState } from './types'

export type CondType =
  | 'level_at_least'
  | 'power_at_least'
  | 'streak_at_least'
  | 'healthy_days_at_least'
  | 'sessions_today_at_least'
  | 'and'
  | 'or'

export interface Cond {
  type: CondType
  value?: number
  of?: Cond[]
}

/** View "phẳng" fed vào registry — gộp core + state các hệ. */
export interface CondView {
  level: number
  power: number
  streak: number
  healthyDays: number
  todaySessions: number
  totalSessions: number
}

export function condView(state: GameState): CondView {
  return {
    level: state.pet.level,
    power: state.systems.power.value,
    streak: state.pet.streak,
    healthyDays: state.systems.stats.healthyDays,
    todaySessions: state.pet.todaySessions,
    totalSessions: state.systems.stats.totalSessions,
  }
}

export const COND: Record<CondType, (view: CondView, c: Cond) => boolean> = {
  level_at_least: (v, c) => v.level >= (c.value ?? 0),
  power_at_least: (v, c) => v.power >= (c.value ?? 0),
  streak_at_least: (v, c) => v.streak >= (c.value ?? 0),
  healthy_days_at_least: (v, c) => v.healthyDays >= (c.value ?? 0),
  sessions_today_at_least: (v, c) => v.todaySessions >= (c.value ?? 0),
  and: (v, c) => (c.of ?? []).every((x) => evalCond(v, x)),
  or: (v, c) => (c.of ?? []).some((x) => evalCond(v, x)),
}

export function evalCond(view: CondView, c: Cond): boolean {
  return COND[c.type](view, c)
}

/** Text tiếng Việt thân thiện cho điều kiện (UI render từ registry, không hard-code). */
export function describeCond(c: Cond): string {
  switch (c.type) {
    case 'level_at_least': return `Đạt Lv.${c.value} trở lên`
    case 'power_at_least': return `Sức mạnh ${c.value ?? 0}+`
    case 'streak_at_least': return `Streak ${c.value ?? 0} ngày+`
    case 'healthy_days_at_least': return `Khỏe mạnh ${c.value} ngày liền`
    case 'sessions_today_at_least': return `${c.value} phiên hôm nay`
    case 'and': return (c.of ?? []).map(describeCond).join(' và ')
    case 'or': return (c.of ?? []).map(describeCond).join(' hoặc ')
  }
}

/** Tiến độ đạt điều kiện (cur/target) — cho progress bar. Với and/or: lấy con tiến nhất. */
export function condProgress(view: CondView, c: Cond): { cur: number; target: number } {
  switch (c.type) {
    case 'level_at_least': return { cur: view.level, target: c.value ?? 0 }
    case 'power_at_least': return { cur: view.power, target: c.value ?? 0 }
    case 'streak_at_least': return { cur: view.streak, target: c.value ?? 0 }
    case 'healthy_days_at_least': return { cur: view.healthyDays, target: c.value ?? 0 }
    case 'sessions_today_at_least': return { cur: view.todaySessions, target: c.value ?? 0 }
    case 'and':
    case 'or': {
      const kids = (c.of ?? []).map((k) => condProgress(view, k))
      if (!kids.length) return { cur: 1, target: 1 }
      const ratio = (p: { cur: number; target: number }) => p.cur / Math.max(1, p.target)
      return kids.reduce((a, b) => (ratio(b) > ratio(a) ? b : a))
    }
  }
}

/* ---------- Helper dựng điều kiện (catalog đọc cho gọn) ---------- */
export const cLevel = (v: number): Cond => ({ type: 'level_at_least', value: v })
export const cPower = (v: number): Cond => ({ type: 'power_at_least', value: v })
export const cStreak = (v: number): Cond => ({ type: 'streak_at_least', value: v })
export const cHealthyDays = (v: number): Cond => ({ type: 'healthy_days_at_least', value: v })
export const cSessionsToday = (v: number): Cond => ({ type: 'sessions_today_at_least', value: v })
export const cAnd = (...of: Cond[]): Cond => ({ type: 'and', of })
export const cOr = (...of: Cond[]): Cond => ({ type: 'or', of })
