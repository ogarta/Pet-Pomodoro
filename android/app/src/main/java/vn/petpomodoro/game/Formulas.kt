package vn.petpomodoro.game

/**
 * Formulas.kt — toàn bộ công thức số của spec `docs/gameplay-spec.md` §3.
 * CHUNG, không đổi giữa các kịch bản — KHÔNG hard-code số này nơi khác.
 */
object Formulas {

    /** XP lên mức: need(L) = 80 + 30×L → Lv.4→5 cần 200 XP */
    fun xpNeed(level: Int): Int = 80 + 30 * level

    /** Cap Sức mạnh theo level: cap(L) = min(100, 40 + 6×L) → Lv.4 cap 64, Lv.8 cap 88 */
    fun powerCap(level: Int): Int = minOf(100, 40 + 6 * level)

    /** +1 XP / phút focus */
    const val XP_PER_FOCUS_MINUTE = 1

    /** +5 XP / lần cho ăn */
    const val XP_PER_FEED = 5

    /** +8 Sức mạnh / phiên focus hoàn thành */
    const val POWER_PER_SESSION = 8

    /** Sushi: +1 / phiên hoàn thành; cho ăn tốn 1 sushi */
    const val SUSHI_PER_SESSION = 1
    const val SUSHI_PER_FEED = 1

    /** Sushi khởi đầu (fixture spec §4: "khởi đầu 12 sushi, ăn 1 lần → 11") */
    const val SUSHI_START = 12

    /** Mục tiêu ngày: ≥ 3 phiên focus */
    const val GOAL_SESSIONS_PER_DAY = 3

    /** Decay Sức mạnh: ngày không phiên −6; ngày đủ mục tiêu −2; ngày tập dưới mục tiêu: 0 */
    const val POWER_DECAY_IDLE_DAY = -6
    const val POWER_DECAY_GOAL_DAY = -2

    /** Thể trạng: +15 khi cho ăn; −10 mỗi ngày không ăn & không phiên */
    const val CONDITION_PER_FEED = 15
    const val CONDITION_IDLE_DAY_PENALTY = -10

    /** Thời lượng phiên (app pomodoro thật) */
    const val FOCUS_MINUTES = 25
    const val BREAK_MINUTES = 5

    /**
     * Decay Sức mạnh cuối ngày theo §3:
     * ngày không phiên nào → −6 · ngày đủ mục tiêu (≥3 phiên) → −2 · ngày có tập nhưng dưới mục tiêu → 0.
     * (Spec chỉ định nghĩa 2 mức đầu; mức giữa chọn 0 — ghi nhận trong báo cáo.)
     */
    fun powerDecayForDay(sessionsToday: Int): Int = when {
        sessionsToday <= 0 -> POWER_DECAY_IDLE_DAY
        sessionsToday >= GOAL_SESSIONS_PER_DAY -> POWER_DECAY_GOAL_DAY
        else -> 0
    }

    /** Decay Thể trạng cuối ngày: −10 nếu ngày đó không ăn & không phiên */
    fun conditionDeltaForDay(fedToday: Boolean, hadSessionToday: Boolean): Int =
        if (!fedToday && !hadSessionToday) CONDITION_IDLE_DAY_PENALTY else 0
}

/** Trạng thái thể trạng theo band §3: ≥70 Khỏe mạnh · 30–69 Bình thường · <30 Ốm yếu */
enum class ConditionBand(val label: String) {
    KHOE_MANH("Khỏe mạnh"),
    BINH_THUONG("Bình thường"),
    OM_YEU("Ốm yếu");

    companion object {
        fun of(condition: Int): ConditionBand = when {
            condition >= 70 -> KHOE_MANH
            condition >= 30 -> BINH_THUONG
            else -> OM_YEU
        }
    }
}
