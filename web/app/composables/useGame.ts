/**
 * useGame — cầu nối Vue ↔ engine: state đơn nhất (singleton), persist ngay mỗi lần
 * mutation, timer pomodoro THẬT (độ dài theo settings user, bám timestamp nên reload
 * không mất phiên — hết giờ lúc đóng tab vẫn được tính), chu kỳ nghỉ dài sau N phiên
 * focus, hàng đợi fx (overlay tiến hoá, toast).
 */
import { ref, computed } from 'vue'
import type { AlertSettings, ElementId, GameEvent, GameState, TimerSettings } from '#shared/game/types'
import {
  addDaysKey,
  breakMsOf,
  focusMsOf,
  isLongBreakDue,
  nextDayKey,
  todayKey,
  xpNeed,
} from '#shared/game/formulas'
import { applyElapsedDays, devCompleteSession, devGrantXp, devNextDay, newGame, reduce } from '#shared/game/engine'
import { LocalStorageAdapter, docsToState, stateToDocs, type PersistenceAdapter, type SerializedDocs } from '#shared/game/persist'
import { lineById } from '#shared/game/catalog'
import { notifyIfHidden, playChime } from '~/utils/alerts'

const adapter: PersistenceAdapter = new LocalStorageAdapter()

const ready = ref(false)
const state = ref<GameState | null>(null)
/**
 * Nguồn chân lý RAW (không reactive) cho engine: structuredClone không clone được
 * Vue reactive Proxy, nên mọi thao tác engine đi qua rawState; `state` chỉ là bản chiếu reactive.
 */
let rawState: GameState | null = null
const toasts = ref<{ id: number; text: string }[]>([])
const evolvedFx = ref<{ from: string; to: string } | null>(null)
/** Flash tiêu đề tab khi hết giờ (6s) — index.vue đọc để đổi document.title. */
const finishedFlash = ref(false)

let toastSeq = 1
let loopStarted = false
let initStarted = false

function pushToast(text: string): void {
  const id = toastSeq++
  toasts.value.push({ id, text })
  setTimeout(() => {
    toasts.value = toasts.value.filter((t) => t.id !== id)
  }, 3600)
}

function ingestEvents(events: GameEvent[]): void {
  for (const e of events) {
    if (e.type === 'evolved') {
      // giữ nguyên STAGE ID — EvoOverlay tự tra catalog để lấy tên + sprite
      evolvedFx.value = { from: e.from, to: e.to }
      if (rawState?.meta.alerts.sound !== false) playChime('evolve')
    }
    if (e.type === 'session_complete') {
      pushToast(`Hoàn thành ${e.minutes} phút · +${e.minutes} XP · +8 Sức mạnh · +1 sushi`)
    }
    if (e.type === 'level_up') {
      // gộp toast lên level: chỉ giữ toast mới nhất (tránh spam khi nhận nhiều level)
      toasts.value = toasts.value.filter((t) => !t.text.startsWith('Lên Lv.'))
      pushToast(`Lên Lv.${e.to}! Còn ${xpNeed(e.to)} XP nữa tới Lv.${e.to + 1}.`)
    }
    if (e.type === 'branch_changed' && e.branchId) pushToast('Form nhánh Stage 3 đã mở!')
  }
}

async function persist(s: GameState): Promise<void> {
  await adapter.saveDocs(stateToDocs(s))
}

function commit(s: GameState, events: GameEvent[]): void {
  rawState = s
  state.value = s
  ingestEvents(events)
  void persist(s)
}

/** Khôi phục timer thật: hết giờ khi vắng mặt → vẫn tính phiên (real app behavior). */
function restoreTimer(s: GameState): { state: GameState; events: GameEvent[] } {
  const t = s.meta.timer
  const events: GameEvent[] = []
  if (!t) return { state: s, events }
  if (!t.running) return { state: s, events }
  if (t.endAt !== null && t.endAt <= Date.now()) {
    if (t.phase === 'focus') {
      // XP bám độ dài phiên lúc bắt đầu (totalMs), không bám settings hiện tại.
      const minutes = plannedMinutes(t)
      const r = reduce(s, { type: 'session_complete', minutes, source: 'focus' })
      r.state.meta.timer = null
      r.state.meta.focusStreak += 1
      events.push(...r.events)
      pushToast(`Vắng mặt vẫn đủ ${minutes} phút — phiên được tính!`)
      return { state: r.state, events }
    }
    s.meta.timer = null // break hết lúc vắng → coi như xong nghỉ
    pushToast('Hết giờ nghỉ — quay lại tập tiếp nào!')
    return { state: s, events }
  }
  return { state: s, events }
}

async function init(): Promise<void> {
  if (initStarted) return
  initStarted = true
  if (import.meta.server) {
    // Server không biết localStorage → render splash, tránh hydration mismatch.
    return
  }
  const docs = await adapter.loadDocs()
  if (docs) {
    const loaded = docsToState(docs)
    if (loaded) {
      const day = applyElapsedDays(loaded, todayKey()) // real decay theo ngày thật
      const rt = restoreTimer(day.state)
      commit(rt.state, [...day.events, ...rt.events])
    }
  }
  ready.value = true
}

/* ---------- Timer thật ---------- */
const nowMs = ref(Date.now())

function startLoop(): void {
  if (loopStarted || import.meta.server) return
  loopStarted = true
  window.setInterval(() => {
    nowMs.value = Date.now()
    const s = rawState
    const t = s?.meta.timer
    if (s && t?.running && t.endAt !== null && t.endAt <= Date.now()) finishTimerNaturally(s)
  }, 400)
}

/** Phút XP của phiên = độ dài phiên lúc bắt đầu (totalMs), tối thiểu 1 phút. */
function plannedMinutes(t: { totalMs: number }): number {
  return Math.max(1, Math.round(t.totalMs / 60_000))
}

/** Hết giờ thật: flash tiêu đề + chuông + thông báo (nếu tab đang ẩn) — tôn trọng alerts. */
function fireTimerAlerts(focusDone: boolean, minutes: number): void {
  finishedFlash.value = true
  window.setTimeout(() => {
    finishedFlash.value = false
  }, 6000)
  if (rawState?.meta.alerts.sound !== false) playChime(focusDone ? 'focusDone' : 'breakDone')
  if (rawState?.meta.alerts.notify !== false) {
    void notifyIfHidden(
      focusDone ? 'Bé xong phiên rồi! 🎉' : 'Hết giờ nghỉ! ☕',
      focusDone
        ? `${minutes} phút tập hoàn thành — giờ nghỉ một chút nhé!`
        : 'Tỉnh dậy nào — quay lại tập tiếp thôi!',
    )
  }
}

function finishTimerNaturally(s: GameState): void {
  const t = s.meta.timer
  if (!t) return
  if (t.phase === 'focus') {
    const minutes = plannedMinutes(t)
    const r = reduce(s, { type: 'session_complete', minutes, source: 'focus' })
    r.state.meta.timer = null
    r.state.meta.focusStreak += 1 // đủ 1 phiên → đếm chuỗi cho nghỉ dài
    commit(r.state, r.events)
    fireTimerAlerts(true, minutes)
  } else {
    const s2 = structuredClone(s)
    s2.meta.timer = null
    commit(s2, [])
    fireTimerAlerts(false, 0)
    pushToast('Hết giờ nghỉ — quay lại tập tiếp nào!')
  }
}

function setTimer(phase: 'focus' | 'break', running: boolean, remainingMs: number, totalMs: number): void {
  const s = rawState ? structuredClone(rawState) : null
  if (!s) return
  s.meta.timer = running
    ? { phase, running: true, remainingMs, endAt: Date.now() + remainingMs, totalMs }
    : { phase, running: false, remainingMs, endAt: null, totalMs }
  finishedFlash.value = false // bấm mới → tắt flash "hết giờ" cũ
  commit(s, [])
}

/* ---------- API công khai ---------- */
export function useGame() {
  startLoop()
  void init()

  const pet = computed(() => state.value?.pet ?? null)
  const hasPet = computed(() => state.value !== null)

  const timer = computed(() => state.value?.meta.timer ?? null)
  const settings = computed(() => state.value?.meta.settings ?? null)
  const alerts = computed(() => state.value?.meta.alerts ?? { sound: true, notify: true })
  const focusStreak = computed(() => state.value?.meta.focusStreak ?? 0)
  /** Đã đủ chuỗi phiên focus → lần nghỉ kế tiếp là NGHỈ DÀI. */
  const longBreakDue = computed(() => {
    const s = state.value
    return s ? isLongBreakDue(s.meta.settings, s.meta.focusStreak) : false
  })
  const remainingMs = computed(() => {
    nowMs.value // phụ thuộc tick
    const t = state.value?.meta.timer
    if (!t) return null
    if (!t.running) return t.remainingMs
    return Math.max(0, (t.endAt ?? 0) - nowMs.value)
  })

  function mmss(ms: number): string {
    const total = Math.max(0, Math.round(ms / 1000))
    const m = Math.floor(total / 60)
    const sec = total % 60
    return `${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
  }

  function hatch(speciesId: ElementId): void {
    const line = lineById(speciesId)
    commit(newGame(speciesId, todayKey()), [])
    pushToast(`Chào mừng ${line.stages[0].name} về nhà! Bắt đầu phiên tập đầu tiên nhé.`)
  }

  /** Cho ăn: −1 sushi · +5 XP · +15 thể trạng. false = hết sushi. */
  function feedPet(): boolean {
    const s = rawState
    if (!s) return false
    const r = reduce(s, { type: 'feed' })
    if (r.rejected) return false
    commit(r.state, r.events)
    if (s.meta.alerts.sound !== false) playChime('feed')
    return true
  }

  function dismissEvolvedFx(): void {
    evolvedFx.value = null
  }

  /* timer controls — độ dài phiên bám settings hiện tại; phiên đang chạy giữ totalMs cũ */
  const startFocus = () => {
    const s = rawState
    if (!s) return
    setTimer('focus', true, focusMsOf(s.meta.settings), focusMsOf(s.meta.settings))
  }
  const startBreak = (long = false) => {
    const s = rawState
    if (!s) return
    // Bắt đầu nghỉ (ngắn hay dài) → chuỗi phiên đếm lại từ 0.
    const s2 = structuredClone(s)
    s2.meta.focusStreak = 0
    s2.meta.timer = null
    commit(s2, [])
    setTimer('break', true, breakMsOf(s.meta.settings, long), breakMsOf(s.meta.settings, long))
  }
  const pauseTimer = () => {
    const t = rawState?.meta.timer
    if (t?.running) setTimer(t.phase, false, Math.max(0, (t.endAt ?? 0) - Date.now()), t.totalMs)
  }
  const resumeTimer = () => {
    const t = rawState?.meta.timer
    if (t && !t.running) setTimer(t.phase, true, t.remainingMs, t.totalMs)
  }
  const abortTimer = () => {
    const s = rawState ? structuredClone(rawState) : null
    if (!s) return
    s.meta.timer = null
    commit(s, [])
  }

  /** Cài đặt pomodoro: clamp + persist. Phiên đang chạy không đổi (bám totalMs riêng). */
  function updateSettings(patch: Partial<TimerSettings>): void {
    const s = rawState
    if (!s) return
    const s2 = structuredClone(s)
    s2.meta.settings = { ...s2.meta.settings, ...patch }
    commit(s2, [])
  }

  /** Tuỳ chọn âm thanh / thông báo (doc state/meta → alerts). */
  function updateAlerts(patch: Partial<AlertSettings>): void {
    const s = rawState
    if (!s) return
    const s2 = structuredClone(s)
    s2.meta.alerts = { ...s2.meta.alerts, ...patch }
    commit(s2, [])
  }

  /** Bản lưu JSON (shape Firestore docs) — file giữ phòng mất dữ liệu, dùng chéo được với Android. */
  function exportDocs(): SerializedDocs | null {
    return rawState ? stateToDocs(rawState) : null
  }

  /**
   * Nhập bản lưu: validate chặt qua docsToState (sai shape → false, KHÔNG đụng state hiện tại).
   * Hợp lệ → chấm decay theo ngày thật + khôi phục timer rồi ghi đè.
   */
  function importDocs(docs: unknown): boolean {
    if (typeof docs !== 'object' || docs === null) return false
    const loaded = docsToState(docs as SerializedDocs)
    if (!loaded) return false
    const day = applyElapsedDays(loaded, todayKey())
    const rt = restoreTimer(day.state)
    commit(rt.state, [...day.events, ...rt.events])
    return true
  }

  /* DEV QA (?dev=1) */
  const dev = {
    completeSession() {
      const s = rawState
      if (!s) return
      const r = devCompleteSession(s)
      if (r.state.meta.timer?.phase === 'focus') r.state.meta.timer = null
      commit(r.state, r.events)
    },
    nextDay() {
      const s = rawState
      if (!s) return
      const r = devNextDay(s)
      commit(r.state, r.events)
      pushToast(`[DEV] Qua ngày mới → ${nextDayKey(s.meta.today)} (decay & streak đã chấm)`)
    },
    addXp(amount = 1000) {
      const s = rawState
      if (!s) return
      const r = devGrantXp(s, amount)
      commit(r.state, r.events)
    },
    async reset() {
      await adapter.deleteAll()
      rawState = null
      state.value = null
      evolvedFx.value = null
      ready.value = true
    },
  }

  /** mốc ngày cho stats (flame cells) — hôm nay ngược về 13 ngày. */
  function dayKeysBack(n: number): string[] {
    const today = rawState?.meta.today ?? todayKey()
    return Array.from({ length: n }, (_, i) => addDaysKey(today, -(n - 1 - i)))
  }

  return {
    ready,
    state,
    pet,
    hasPet,
    timer,
    remainingMs,
    settings,
    alerts,
    updateAlerts,
    exportDocs,
    importDocs,
    finishedFlash,
    focusStreak,
    longBreakDue,
    mmss,
    toasts,
    notify: pushToast,
    evolvedFx,
    dismissEvolvedFx,
    hatch,
    feedPet,
    startFocus,
    startBreak,
    pauseTimer,
    resumeTimer,
    abortTimer,
    updateSettings,
    dev,
    dayKeysBack,
  }
}
