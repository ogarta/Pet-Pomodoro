package vn.petpomodoro.game

/**
 * Conditions.kt — Registry điều kiện dùng chung (spec §1.3), combo AND/OR.
 *
 * Thêm LOẠI điều kiện mới = thêm 1 data class ở đây (đúng tinh thần
 * `registerConditionType(key, fn)` của spec — Kotlin sealed interface
 * thay cho bảng COND bằng key), KHÔNG phải sửa hệ cũ.
 *
 * Mọi điều kiện biết:
 *  - [evaluate]  chống lên [GameState] (core §1.1 + trường hệ đăng ký),
 *  - [describe]  chuỗi tiếng Việt cho UI (thẻ nhánh, checklist Stats…),
 *  - [leaves]    các lá để UI vẽ progress "62/85" như demo KB1.
 */
sealed interface Condition {

    fun evaluate(s: GameState): Boolean

    fun describe(): String

    /** Các điều kiện lá (kết quả tính hiện tại + mục tiêu) cho progress bar */
    fun leaves(s: GameState): List<LeafProgress>

    data class LevelAtLeast(val value: Int) : Condition {
        override fun evaluate(s: GameState) = s.level >= value
        override fun describe() = "Lv ≥ $value"
        override fun leaves(s: GameState) =
            listOf(LeafProgress(describe(), s.level, value))
    }

    data class PowerAtLeast(val value: Int) : Condition {
        override fun evaluate(s: GameState) = s.power >= value
        override fun describe() = "Sức mạnh ≥ $value"
        override fun leaves(s: GameState) =
            listOf(LeafProgress(describe(), s.power, value))
    }

    data class StreakAtLeast(val value: Int) : Condition {
        override fun evaluate(s: GameState) = s.streak >= value
        override fun describe() = "streak ≥ $value ngày"
        override fun leaves(s: GameState) =
            listOf(LeafProgress(describe(), s.streak, value))
    }

    data class HealthyDaysAtLeast(val value: Int) : Condition {
        override fun evaluate(s: GameState) = s.healthyDays >= value
        override fun describe() = "Khỏe mạnh liên tục ≥ $value ngày"
        override fun leaves(s: GameState) =
            listOf(LeafProgress(describe(), s.healthyDays, value))
    }

    data class SessionsTodayAtLeast(val value: Int) : Condition {
        override fun evaluate(s: GameState) = s.todaySessions >= value
        override fun describe() = "phiên hôm nay ≥ $value"
        override fun leaves(s: GameState) =
            listOf(LeafProgress(describe(), s.todaySessions, value))
    }

    data class And(val of: List<Condition>) : Condition {
        override fun evaluate(s: GameState) = of.all { it.evaluate(s) }
        override fun describe() = of.joinToString(" ∧ ") { it.describe() }
        override fun leaves(s: GameState) = of.flatMap { it.leaves(s) }
    }

    data class Or(val of: List<Condition>) : Condition {
        override fun evaluate(s: GameState) = of.any { it.evaluate(s) }
        override fun describe() = of.joinToString(" ∨ ") { it.describe() }
        override fun leaves(s: GameState) = of.flatMap { it.leaves(s) }
    }
}

/** Tiến độ 1 điều kiện lá, render thành progress bar như demo KB1 (62/85 · 7/10) */
data class LeafProgress(val label: String, val current: Int, val target: Int) {
    val fraction: Float get() = if (target <= 0) 1f else (current.toFloat() / target).coerceIn(0f, 1f)
    val met: Boolean get() = current >= target
}
