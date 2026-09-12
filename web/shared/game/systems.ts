/**
 * PetPomodoro — Hệ thống module + event bus (spec §1.1–§1.2).
 * Core chỉ có event bus: session_complete / feed / day_end / level_up / status_change.
 * Mỗi hệ = 1 khai báo { id, on, decay, ui } — UI đọc SYSTEMS[].ui để biết vẽ gì ở slot nào.
 *
 * Ngày mai thêm hệ Nhiệm vụ hằng ngày — KHÔNG đụng core (spec §1.6):
 *   registerSystem({ id:'daily_quests', on:{ session_complete: quests.check, feed: quests.check },
 *                    ui:[{ slot:'home_bottom', type:'quest_list' }] })
 */
import type { GameEvent, GameEventType, GameState } from './types'
import {
  CONDITION_PER_FEED,
  COIN_PER_FEED,
  COIN_PER_SESSION,
  DECAY_CONDITION_IDLE,
  DECAY_POWER_GOAL,
  DECAY_POWER_IDLE,
  GOAL_SESSIONS,
  POWER_PER_SESSION,
  XP_PER_FEED,
  XP_PER_FOCUS_MINUTE,
  clamp,
  powerCap,
  statusBand,
  xpNeed,
} from './formulas'
import { condView, evalCond } from './conditions'
import { lineById, stageAtLevel } from './catalog'

export interface SystemUi {
  slot: string
  type: string
  label?: string
}

/** Handler nhận event đã narrow theo key. */
export type EventHandler<K extends GameEventType = GameEventType> = (
  s: GameState,
  e: GameEvent & { type: K },
) => void

export interface SystemModule {
  id: string
  description: string
  on?: { [K in GameEventType]?: EventHandler<K> }
  decay?: { perIdleDay: number; perGoalDay?: number }
  emits?: GameEventType[]
  ui?: SystemUi[]
}

/* ---------- Event bus: dispatch() + emit() ---------- */

let activeQueue: GameEvent[] | null = null
let collected: GameEvent[] | null = null

function emit(e: GameEvent): void {
  activeQueue?.push(e)
}

/**
 * dispatch(state, event): chạy mọi handler củaevent (và sự kiện phát sinh dây chuy):
 * level_up → evolution → evolved/branch_changed … Kết quả trả về gồm TẤT CẢ sự kiện đã xử lý
 * (UI dùng để dựng fx: overlay tiến hoá, toast…).
 */
export function dispatch(state: GameState, event: GameEvent): GameEvent[] {
  if (activeQueue) {
    activeQueue.push(event) // dispatch lồng nhau → xếp vào hàng đang chạy
    return []
  }
  activeQueue = [event]
  collected = []
  let guard = 0
  while (activeQueue.length && guard++ < 50) {
    const ev = activeQueue.shift() as GameEvent
    for (const sys of SYSTEMS) {
      const handlers = sys.on as Partial<Record<GameEventType, ((s: GameState, e: GameEvent) => void) | undefined>> | undefined
      handlers?.[ev.type]?.(state, ev)
    }
    collected.push(ev)
  }
  const out = collected
  activeQueue = null
  collected = null
  return out
}

/* ---------- Handlers ---------- */

/** Cộng XP + xử lý lên mức (export cho engine — dev grant XP). */
export function runXpGain(s: GameState, amount: number): GameEvent[] {
  const out: GameEvent[] = []
  s.pet.xp += amount
  s.systems.stats.totalXp += amount
  s.systems.stats.todayXp += amount
  while (s.pet.xp >= xpNeed(s.pet.level)) {
    const from = s.pet.level
    s.pet.xp -= xpNeed(from)
    s.pet.level = from + 1
    out.push(...dispatch(s, { type: 'level_up', from, to: from + 1 }))
  }
  return out
}

function setCondition(s: GameState, v: number): void {
  const from = statusBand(s.pet.condition)
  s.pet.condition = clamp(v, 0, 100)
  const to = statusBand(s.pet.condition)
  if (from !== to) emit({ type: 'status_change', from, to })
}

function unlockForm(s: GameState, formId: string): void {
  if (!s.systems.evolution.unlocked.includes(formId)) s.systems.evolution.unlocked.push(formId)
}

/** Stage theo level — Lv.8 / Lv.16 vượt ngưỡng thật → tiến hoá. */
function syncStage(s: GameState): void {
  const line = lineById(s.pet.speciesId)
  const st = stageAtLevel(line, s.pet.level)
  if (st.id !== s.pet.stageId) {
    const from = s.pet.stageId
    s.pet.stageId = st.id
    unlockForm(s, st.id)
    emit({ type: 'evolved', from, to: st.id })
  }
  syncBranch(s)
}

/** Nhánh Stage 3: điều kiện registry chấm trên state thật (spec §2.2). */
function syncBranch(s: GameState): void {
  const line = lineById(s.pet.speciesId)
  const view = condView(s)
  for (const b of line.branches) {
    if (evalCond(view, b.cond)) {
      if (s.systems.evolution.branchId !== b.id) {
        s.systems.evolution.branchId = b.id
        unlockForm(s, `${line.id}-${b.id}`)
        emit({ type: 'branch_changed', branchId: b.id })
      }
      return // nhánh đầu tiên đạt được giữ nguyên (Hiền ưu tiên theo thứ tự catalog)
    }
  }
}

/* ---------- Khai báo các hệ (data-driven — thêm hệ = thêm entry) ---------- */

export const SYSTEMS: SystemModule[] = [
  {
    id: 'level',
    description: 'Kinh nghiệm & cấp — mỗi phút tập +1 XP, mỗi lần ăn +5 XP',
    on: {
      session_complete: (s, e) => { runXpGain(s, e.minutes * XP_PER_FOCUS_MINUTE) },
      feed: (s) => { runXpGain(s, XP_PER_FEED) },
    },
    emits: ['level_up'],
    ui: [{ slot: 'home', type: 'xp_bar', label: 'XP' }],
  },
  {
    id: 'power',
    description: 'Sức mạnh — mạnh lên sau mỗi phiên tập, yếu dần nếu bỏ bê',
    on: {
      session_complete: (s) => {
        const cap = powerCap(s.pet.level)
        s.systems.power.value = clamp(s.systems.power.value + POWER_PER_SESSION, 0, cap)
      },
      day_end: (s, e) => {
        if (e.sessions >= GOAL_SESSIONS) {
          s.systems.power.value = clamp(s.systems.power.value + DECAY_POWER_GOAL, 0, 100)
        } else if (e.sessions === 0) {
          s.systems.power.value = clamp(s.systems.power.value + DECAY_POWER_IDLE, 0, 100)
        } // 1–2 phiên: không decay (quy ước M1)
      },
    },
    decay: { perIdleDay: DECAY_POWER_IDLE, perGoalDay: DECAY_POWER_GOAL },
    emits: [],
    ui: [{ slot: 'home', type: 'meter', label: 'Sức mạnh' }],
  },
  {
    id: 'vitals',
    description: 'Thể trạng — cho ăn để bé khỏe, bỏ bê lâu bé ốm',
    on: {
      feed: (s) => { setCondition(s, s.pet.condition + CONDITION_PER_FEED) },
      day_end: (s, e) => {
        if (e.sessions === 0 && e.fed === 0) setCondition(s, s.pet.condition + DECAY_CONDITION_IDLE)
      },
    },
    decay: { perIdleDay: DECAY_CONDITION_IDLE },
    emits: ['status_change'],
    ui: [{ slot: 'home', type: 'hud', label: 'Thể trạng' }],
  },
  {
    id: 'economy',
    description: 'Sushi — thưởng mỗi phiên tập, dùng để cho bé ăn',
    on: {
      session_complete: (s) => { s.pet.coins += COIN_PER_SESSION },
      feed: (s) => {
        s.pet.coins -= COIN_PER_FEED
        s.systems.stats.totalFeeds += 1
        s.systems.stats.todayFed += 1
      },
    },
    emits: [],
  },
  {
    id: 'streak',
    description: 'Streak — chuỗi ngày liên tục có ít nhất 1 phiên tập',
    on: {
      session_complete: (s) => {
        if (s.systems.stats.lastSessionDate !== s.meta.today) {
          const y = prevDayOf(s.meta.today)
          s.pet.streak = s.systems.stats.lastSessionDate === y ? s.pet.streak + 1 : 1
        }
        s.systems.stats.lastSessionDate = s.meta.today
      },
      day_end: (s, e) => {
        if (e.sessions === 0) s.pet.streak = 0
      },
    },
    emits: [],
  },
  {
    id: 'evolution',
    description: 'Tiến hoá — đủ level là bé đổi form mới; nhánh Stage 3 theo điều kiện',
    on: {
      level_up: (s) => { syncStage(s) },
      session_complete: (s) => { syncBranch(s) },
    },
    emits: ['evolved', 'branch_changed'],
    ui: [
      { slot: 'evolve', type: 'evo_timeline', label: 'Timeline tiến hoá' },
      { slot: 'stats', type: 'evo_tree', label: 'Cây tiến hoá mini' },
    ],
  },
  {
    id: 'collection',
    description: 'Sưu tầm — Pokédex form lai hiếm (sắp mở)',
    ui: [{ slot: 'stats', type: 'pokedex', label: 'Pokédex' }],
  },
]

function prevDayOf(key: string): string {
  const d = new Date(`${key}T12:00:00`)
  d.setDate(d.getDate() - 1)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/** UI helper: mọi khai báo ui của 1 slot (component render từ config, spec §1.2). */
export function systemsUi(slot: string): SystemUi[] {
  return SYSTEMS.flatMap((s) => s.ui ?? []).filter((u) => u.slot === slot)
}
