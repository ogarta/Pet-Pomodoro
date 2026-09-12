package vn.petpomodoro.game

/**
 * GameEvents.kt — Event bus của core (spec §1.1):
 * `session_complete(minutes)` · `feed` · `day_end` · `level_up` · `status_change`
 * (+ `evolved`, `form_unlocked` do hệ evolution/collection phát ra).
 *
 * Sự kiện VÀO (UI gọi): [GameEvent.SessionComplete], [GameEvent.Feed], [GameEvent.DayEnd].
 * Sự kiện RA (hệ phát): [GameEvent.LevelUp], [GameEvent.StatusChange], [GameEvent.Evolved],
 * [GameEvent.FormUnlocked] — UI nghe để hiện overlay "Tiến hoá!", banner ốm…,
 * các hệ khác nghe để phản ứng (level_up → evolution check như spec §1.2).
 */
sealed interface GameEvent {

    /** Hoàn thành 1 phiên focus [minutes] phút. [minuteOfDay] = phút trong ngày (ghi lịch sử), -1 = không rõ. */
    data class SessionComplete(val minutes: Int, val minuteOfDay: Int = -1) : GameEvent

    /** Cho ăn 1 sushi (−1 sushi · +5 XP · +15 thể trạng) */
    data object Feed : GameEvent

    /** Kết thúc 1 ngày (app gọi khi đổi ngày lịch; test gọi trực tiếp) */
    data class DayEnd(val epochDay: Long) : GameEvent

    // ─── Sự kiện hệ phát ra ────────────────────────────────────────────
    data class LevelUp(val oldLevel: Int, val newLevel: Int) : GameEvent

    data class StatusChange(val old: ConditionBand, val new: ConditionBand) : GameEvent

    /** Pet đổi form (egg→s1 không phát; s1→s2→s3 phát; [branchId] khác null khi vào nhánh Stage 3) */
    data class Evolved(val fromStageId: String, val toStageId: String, val branchId: String?) : GameEvent

    /** 1 form (hiện tại lai) mở khóa trong sưu tầm */
    data class FormUnlocked(val formId: String) : GameEvent
}

/** 1 dòng lịch sử ngày cho màn Stats */
data class DayRecord(
    val epochDay: Long,
    val minutes: Int,
    val sessions: Int,
    val goalMet: Boolean,
)

/** 1 dòng lịch sử phiên (LỊCH SỬ PHIÊN trên Stats, như demo KB1) */
data class SessionLogEntry(
    val epochDay: Long,
    val minuteOfDay: Int,
    val minutes: Int,
    val xp: Int,
)

/**
 * Core tối thiểu (spec §1.1):
 * `core = { speciesId, stageId, level, xp, condition(0-100), coins, streak, todaySessions, todayMinutes }`
 * + trạng thái hệ đăng ký thêm (power, healthyDays, branchId, unlocked…) và sổ sách ngày.
 * Immutable — hệ trả về bản copy mới.
 */
data class GameState(
    val speciesId: String = "",
    val stageId: String = "",
    val branchId: String? = null,
    val level: Int = 1,
    val xp: Int = 0,
    /** Sức mạnh (hệ `power`) */
    val power: Int = 40,
    /** Thể trạng 0–100 (thuộc core) */
    val condition: Int = 80,
    val coins: Int = Formulas.SUSHI_START,
    val streak: Int = 0,
    /** Số ngày liên tục kết ngày ở band Khỏe mạnh (đăng ký cho registry healthy_days_at_least) */
    val healthyDays: Int = 0,
    val todaySessions: Int = 0,
    val todayMinutes: Int = 0,
    val todayXp: Int = 0,
    val dayFed: Boolean = false,
    val dayHadSession: Boolean = false,
    val lastDayEpochDay: Long = 0L,
    /** Hệ collection: các form đã mở (stage + lai) */
    val unlockedFormIds: Set<String> = emptySet(),
    val history: List<DayRecord> = emptyList(),
    val sessionLog: List<SessionLogEntry> = emptyList(),
) {
    val conditionBand: ConditionBand get() = ConditionBand.of(condition)
    val isSick: Boolean get() = conditionBand == ConditionBand.OM_YEU
}
