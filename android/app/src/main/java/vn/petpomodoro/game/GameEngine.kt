package vn.petpomodoro.game

/**
 * GameEngine.kt — Core + event bus (spec §1.1–1.2).
 *
 * Core KHÔNG biết gì về hệ nào cả: nó chỉ định tuyến sự kiện qua các [GameSystem].
 * Mỗi hệ = 1 khai báo (data-driven, spec §1.2) — thêm hệ mới chỉ là thêm entry trong
 * [defaultSystems], không đụng core hay hệ cũ.
 *
 * Sự kiện vào được đổ vào hàng đợi; hệ có thể phát sự kiện mới (level_up →
 * evolution check) và sẽ được xử lý trong cùng lượt drain — đúng tinh thần
 * `on: { level_up: checkEvolution }` của spec. UI đăng ký [addListener] để nhận
 * mọi sự kiện (hiện overlay tiến hoá, banner ốm…).
 */
interface GameSystem {
    val id: String
    fun onEvent(state: GameState, event: GameEvent, emit: (GameEvent) -> Unit): GameState
}

class GameEngine(
    initialState: GameState,
    private val systems: List<GameSystem> = defaultSystems(),
) {

    private val eventQueue = ArrayDeque<GameEvent>()
    private val listeners = mutableListOf<(GameEvent) -> Unit>()

    var state: GameState = initialState
        private set

    fun addListener(listener: (GameEvent) -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: (GameEvent) -> Unit) {
        listeners.remove(listener)
    }

    /** Đổ 1 sự kiện vào bus và xử lý đến cạn hàng đợi (kể cả sự kiện phát sinh). */
    fun dispatch(event: GameEvent) {
        eventQueue.addLast(event)
        while (eventQueue.isNotEmpty()) {
            val e = eventQueue.removeFirst()
            for (system in systems) {
                state = system.onEvent(state, e, eventQueue::addLast)
            }
            listeners.forEach { it(e) }
        }
    }

    /** `session_complete(minutes)` — app gọi khi phiên focus 25 phút kết thúc. */
    fun sessionComplete(minutes: Int, minuteOfDay: Int = -1) =
        dispatch(GameEvent.SessionComplete(minutes, minuteOfDay))

    /**
     * `feed` — cho ăn 1 sushi: −1 sushi · +5 XP · +15 thể trạng (spec §3).
     * Trả về false nếu không đủ sushi (không cho nợ).
     */
    fun feed(): Boolean {
        if (state.coins < Formulas.SUSHI_PER_FEED) return false
        state = state.copy(coins = state.coins - Formulas.SUSHI_PER_FEED)
        dispatch(GameEvent.Feed)
        return true
    }

    /**
     * Gọi khi app biết hôm nay là [todayEpochDay] (LocalDate.toEpochDay()).
     * Tự động đóng từng ngày bị bỏ lỡ bằng sự kiện `DayEnd` (decay §3 chạy mỗi ngày).
     */
    fun maybeAdvanceDay(todayEpochDay: Long) {
        if (state.lastDayEpochDay == 0L) {
            state = state.copy(lastDayEpochDay = todayEpochDay)
            return
        }
        while (state.lastDayEpochDay < todayEpochDay) {
            dispatch(GameEvent.DayEnd(state.lastDayEpochDay + 1))
        }
    }

    companion object {
        /**
         * Danh sách HỆ (spec §1.2). Ngày mai thêm hệ Nhiệm vụ hằng ngày:
         * `defaultSystems() + DailyQuestSystem()` — KHÔNG đụng core.
         */
        fun defaultSystems(): List<GameSystem> = listOf(
            LevelSystem(),
            PowerSystem(),
            ConditionSystem(),
            EvolutionSystem(),
            CollectionSystem(),
        )

        /** Nhận trứng → tạo state ban đầu (Lv.1, 40 sức mạnh, 80 thể trạng, 12 sushi). */
        fun adopt(lineId: String, epochDay: Long): GameState {
            val line = Catalog.line(lineId)
            val stage = line.stages.first()
            return GameState(
                speciesId = line.id,
                stageId = stage.id,
                level = 1,
                xp = 0,
                power = 40,
                condition = 80,
                coins = Formulas.SUSHI_START,
                lastDayEpochDay = epochDay,
                unlockedFormIds = setOf(stage.id),
            )
        }
    }
}

/* ============================================================================
 * HỆ 1 — level: +1 XP/phút focus · +5 XP/lần ăn → phát `level_up` (spec §1.2)
 * ========================================================================== */
class LevelSystem : GameSystem {
    override val id = "level"

    override fun onEvent(state: GameState, event: GameEvent, emit: (GameEvent) -> Unit): GameState =
        when (event) {
            is GameEvent.SessionComplete -> gainXp(state, event.minutes * Formulas.XP_PER_FOCUS_MINUTE, emit)
            is GameEvent.Feed -> gainXp(state, Formulas.XP_PER_FEED, emit)
            else -> state
        }

    private fun gainXp(state: GameState, amount: Int, emit: (GameEvent) -> Unit): GameState {
        var s = state.copy(xp = state.xp + amount, todayXp = state.todayXp + amount)
        while (s.xp >= Formulas.xpNeed(s.level)) {
            val old = s.level
            s = s.copy(xp = s.xp - Formulas.xpNeed(old), level = old + 1)
            emit(GameEvent.LevelUp(old, old + 1))
        }
        return s
    }
}

/* ============================================================================
 * HỆ 2 — power: +8/phiên (theo cap theo level) · decay ngày không phiên −6,
 * ngày đủ mục tiêu −2 (spec §3). Chạy SAU level nên cap dùng level mới.
 * ========================================================================== */
class PowerSystem : GameSystem {
    override val id = "power"

    override fun onEvent(state: GameState, event: GameEvent, emit: (GameEvent) -> Unit): GameState =
        when (event) {
            is GameEvent.SessionComplete ->
                state.copy(power = (state.power + Formulas.POWER_PER_SESSION).coerceAtMost(Formulas.powerCap(state.level)))
            is GameEvent.DayEnd ->
                state.copy(power = (state.power + Formulas.powerDecayForDay(state.todaySessions)).coerceAtLeast(0))
            else -> state
        }
}

/* ============================================================================
 * HỆ 3 — condition + streak + healthyDays: thể trạng thuộc CORE nhưng
 * sổ sách ngày (fed/hadSession, streak, healthyDays) nằm ở đây.
 * +15 khi cho ăn · −10/ngày không ăn & không phiên · streak = chuỗi ngày đủ mục tiêu. * ========================================================================== */
class ConditionSystem : GameSystem {
    override val id = "condition"

    override fun onEvent(state: GameState, event: GameEvent, emit: (GameEvent) -> Unit): GameState =
        when (event) {
            is GameEvent.SessionComplete -> state.copy(dayHadSession = true)
            is GameEvent.Feed -> {
                val newCondition = (state.condition + Formulas.CONDITION_PER_FEED).coerceAtMost(100)
                val s = state.copy(condition = newCondition, dayFed = true)
                emitBandChange(state, s, emit)
                s
            }
            is GameEvent.DayEnd -> {
                val newCondition =
                    (state.condition + Formulas.conditionDeltaForDay(state.dayFed, state.dayHadSession))
                        .coerceIn(0, 100)
                val goalMet = state.todaySessions >= Formulas.GOAL_SESSIONS_PER_DAY
                val s = state.copy(
                    condition = newCondition,
                    streak = if (goalMet) state.streak + 1 else 0,
                    healthyDays = if (ConditionBand.of(newCondition) == ConditionBand.KHOE_MANH) state.healthyDays + 1 else 0,
                    history = (state.history + DayRecord(event.epochDay, state.todayMinutes, state.todaySessions, goalMet))
                        .takeLast(MAX_HISTORY),
                    todaySessions = 0,
                    todayMinutes = 0,
                    todayXp = 0,
                    dayFed = false,
                    dayHadSession = false,
                    lastDayEpochDay = event.epochDay,
                )
                emitBandChange(state, s, emit)
                s
            }
            else -> state
        }

    private fun emitBandChange(before: GameState, after: GameState, emit: (GameEvent) -> Unit) {
        if (before.conditionBand != after.conditionBand) {
            emit(GameEvent.StatusChange(before.conditionBand, after.conditionBand))
        }
    }

    companion object {
        const val MAX_HISTORY = 30
    }
}

/* ============================================================================
 * HỆ 4 — evolution: nghe `level_up` → checkEvolution (spec §1.2).
 * egg → s1 (Lv.1) → s2 (Lv.5) → s3 (Lv.16); Stage 3 chọn nhánh đầu tiên
 * thỏa điều kiện registry (Hiền ∨ Chiến Binh), không nhánh nào → form thường.
 * ========================================================================== */
class EvolutionSystem : GameSystem {
    override val id = "evolution"

    override fun onEvent(state: GameState, event: GameEvent, emit: (GameEvent) -> Unit): GameState {
        if (event !is GameEvent.LevelUp || state.speciesId.isEmpty()) return state
        val line = Catalog.line(state.speciesId)
        val target = line.stageFor(state.level)
        if (target.id == state.stageId) return state

        val branch = if (target.atLevel == Catalog.LEVEL_STAGE_3) Catalog.pickBranch(line, state) else null
        emit(GameEvent.Evolved(state.stageId, target.id, branch?.id))
        return state.copy(stageId = target.id, branchId = branch?.id)
    }
}

/* ============================================================================
 * HỆ 5 — collection: mở khóa form lai khi đủ điều kiện kép (spec §2.3).
 * UI Pokédex/lai tạo đọc Catalog + GameState.unlockedFormIds.
 * ========================================================================== */
class CollectionSystem : GameSystem {
    override val id = "collection"

    override fun onEvent(state: GameState, event: GameEvent, emit: (GameEvent) -> Unit): GameState {
        if (state.speciesId.isEmpty()) return state
        if (event !is GameEvent.SessionComplete && event !is GameEvent.LevelUp &&
            event !is GameEvent.Feed && event !is GameEvent.DayEnd
        ) return state

        var s = state
        for (hybrid in Catalog.hybridsFor(state.speciesId)) {
            if (hybrid.id !in s.unlockedFormIds && hybrid.cond.evaluate(s)) {
                s = s.copy(unlockedFormIds = s.unlockedFormIds + hybrid.id)
                emit(GameEvent.FormUnlocked(hybrid.id))
            }
        }
        return s
    }
}
