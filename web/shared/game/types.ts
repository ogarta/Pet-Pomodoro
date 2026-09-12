/**
 * PetPomodoro — Kiểu dữ liệu gameplay core (M1).
 * Shape-state bám spec `docs/gameplay-spec.md` §1.1:
 *   core = { speciesId, stageId, level, xp, condition, coins, streak, todaySessions, todayMinutes }
 * Hệ mở rộng nằm ở `systems/{id}` — thêm hệ mới KHÔNG đụng core (spec §1.5).
 */

export type ElementId = 'lua' | 'thuy' | 'thao'

/** Band thể trạng theo spec §3: ≥70 Khỏe mạnh · 30–69 Bình thường · <30 Ốm yếu */
export type StatusBand = 'khoemanh' | 'binhthuong' | 'omyeu'

/** Firestore: users/{uid}/state/pet */
export interface PetCore {
  speciesId: ElementId
  stageId: string
  level: number
  xp: number
  condition: number
  coins: number
  streak: number
  todaySessions: number
  todayMinutes: number
  hatchedAt: string // ISO
}

/**
 * Cài đặt pomodoro do user chỉnh (Firestore: users/{uid}/state/settings).
 * Đổi cài đặt chỉ áp dụng cho phiên KẾ TIẾP — phiên đang chạy bám totalMs của chính nó.
 */
export interface TimerSettings {
  focusMinutes: number
  breakMinutes: number
  longBreakMinutes: number
  /** Sau N phiên focus liên tục → lần nghỉ kế tiếp là nghỉ dài. */
  sessionsBeforeLongBreak: number
}

/** Bộ đếm timer đang chạy — persist để reload không mất phiên (real app). */
export interface TimerPersist {
  phase: 'focus' | 'break'
  running: boolean
  remainingMs: number
  endAt: number | null
  /** Tổng độ dài phiên lúc bắt đầu — XP & progress bar bám vào đây, không bám settings. */
  totalMs: number
}

/** Tuỳ chọn cảnh báo trên thiết bị (Firestore: users/{uid}/state/meta → alerts). */
export interface AlertSettings {
  /** Âm báo khi hết giờ, cho ăn, tiến hoá. */
  sound: boolean
  /** Thông báo hệ thống khi hết giờ mà tab đang ẩn. */
  notify: boolean
}

/** Firestore: users/{uid}/state/meta — dữ liệu vận hành (ngày, timer, chu kỳ nghỉ). */
export interface GameMeta {
  /** "Hôm nay" theo state — có thể trễ hơn ngày thật nếu user bỏ app. */
  today: string // YYYY-MM-DD
  lastActiveDate: string // YYYY-MM-DD
  timer: TimerPersist | null
  /** Số phiên focus liên tục tính từ lần nghỉ gần nhất (quyết định nghỉ dài). */
  focusStreak: number
  settings: TimerSettings
  alerts: AlertSettings
}

export interface DayRecord {
  date: string // YYYY-MM-DD
  sessions: number
  minutes: number
  xp: number
  goal: boolean // đủ 3 phiên?
}

/** Firestore: users/{uid}/systems/power */
export interface PowerSystemState {
  value: number
}

/** Firestore: users/{uid}/systems/evolution */
export interface EvolutionSystemState {
  unlocked: string[] // các form id đã mở (data-driven cho Pokédex/cây M2)
  branchId: string | null // 'hien' | 'chien-binh' | null (form thường)
}

/** Firestore: users/{uid}/systems/stats */
export interface StatsSystemState {
  todayXp: number
  todayFed: number
  lastSessionDate: string | null
  healthyDays: number // chuỗi ngày liên tục kết ngày ở band Khỏe mạnh
  history: DayRecord[] // tối đa ~30 ngày cuối
  totalSessions: number
  totalMinutes: number
  totalXp: number
  totalFeeds: number
}

export interface GameState {
  pet: PetCore
  meta: GameMeta
  systems: {
    power: PowerSystemState
    evolution: EvolutionSystemState
    stats: StatsSystemState
  }
}

/* ---------- Event bus (spec §1.1: core KHÔNG biết gì về hệ nào) ---------- */

export type GameEventType =
  | 'session_complete'
  | 'feed'
  | 'day_end'
  | 'level_up'
  | 'status_change'
  | 'evolved'
  | 'branch_changed'

export interface GameEventPayloads {
  session_complete: { minutes: number; source: 'focus' | 'dev' }
  feed: Record<string, unknown>
  day_end: { date: string; sessions: number; fed: number }
  level_up: { from: number; to: number }
  status_change: { from: StatusBand; to: StatusBand }
  evolved: { from: string; to: string }
  branch_changed: { branchId: string | null }
}

export type GameEvent = GameEventUnion[keyof GameEventUnion]

interface GameEventUnion {
  session_complete: { type: 'session_complete' } & GameEventPayloads['session_complete']
  feed: { type: 'feed' }
  day_end: { type: 'day_end' } & GameEventPayloads['day_end']
  level_up: { type: 'level_up' } & GameEventPayloads['level_up']
  status_change: { type: 'status_change' } & GameEventPayloads['status_change']
  evolved: { type: 'evolved' } & GameEventPayloads['evolved']
  branch_changed: { type: 'branch_changed' } & GameEventPayloads['branch_changed']
}
