package vn.petpomodoro.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test GameEngine (pure Kotlin, chạy trên JVM):
 *  1. Level up đúng ngưỡng need(L) = 80 + 30×L
 *  2. Tiến hoá: điều kiện ĐỦ / CHƯA ĐỦ (stage Lv.5/Lv.16 + nhánh Stage 3)
 *  3. Decay sau ngày bỏ bê (−6 sức mạnh · −10 thể trạng)
 *  4. Cho ăn hồi phục (+15 thể trạng · StatusChange ốm → thường)
 */
class GameEngineTest {

    private fun engine(state: GameState, log: MutableList<GameEvent>? = null): GameEngine =
        GameEngine(state).also { e -> log?.let { l -> e.addListener { ev -> l.add(ev) } } }

    private fun pet(
        level: Int = 1,
        xp: Int = 0,
        power: Int = 40,
        condition: Int = 80,
        coins: Int = Formulas.SUSHI_START,
        streak: Int = 0,
        healthyDays: Int = 0,
        speciesId: String = "flame",
        stageId: String = "tan-lua",
        branchId: String? = null,
        todaySessions: Int = 0,
        todayMinutes: Int = 0,
        dayFed: Boolean = false,
        dayHadSession: Boolean = false,
        lastDayEpochDay: Long = 100L,
    ) = GameState(
        speciesId = speciesId,
        stageId = stageId,
        branchId = branchId,
        level = level,
        xp = xp,
        power = power,
        condition = condition,
        coins = coins,
        streak = streak,
        healthyDays = healthyDays,
        todaySessions = todaySessions,
        todayMinutes = todayMinutes,
        dayFed = dayFed,
        dayHadSession = dayHadSession,
        lastDayEpochDay = lastDayEpochDay,
    )

    // ── 1. LEVEL UP ───────────────────────────────────────────────────

    @Test
    fun `level up khi xp vượt ngưỡng need(L)`() {
        // Lv.4 → 5 cần 200 XP (80 + 30×4) — fixture spec §3
        val events = mutableListOf<GameEvent>()
        val e = engine(pet(level = 4, xp = 195), events)
        e.sessionComplete(10) // +10 XP/phút → 205 ≥ 200

        assertEquals(5, e.state.level)
        assertEquals(5, e.state.xp) // 205 − 200 dồn sang mức sau
        val ups = events.filterIsInstance<GameEvent.LevelUp>()
        assertTrue(ups.any { it.oldLevel == 4 && it.newLevel == 5 })
    }

    @Test
    fun `một phiên có thể lên nhiều level liên tiếp`() {
        val e = engine(pet(level = 1, xp = 0))
        e.sessionComplete(300) // need(1)=110, need(2)=140 → 300−250 = 50 dư

        assertEquals(3, e.state.level)
        assertEquals(50, e.state.xp)
    }

    @Test
    fun `cho ăn +5 XP cũng tính vào level`() {
        val e = engine(pet(level = 1, xp = 107))
        assertTrue(e.feed()) // +5 → 112 ≥ 110

        assertEquals(2, e.state.level)
        assertEquals(2, e.state.xp)
        assertEquals(Formulas.SUSHI_START - 1, e.state.coins)
    }

    // ── 2. TIẾN HOÁ ───────────────────────────────────────────────────

    @Test
    fun `tiến hoá lên stage 2 đúng Lv8 và phát sự kiện Evolved`() {
        val events = mutableListOf<GameEvent>()
        val e = engine(pet(level = 7, xp = Formulas.xpNeed(7) - 2), events)
        e.sessionComplete(3) // +3 XP → lên Lv.8

        assertEquals(8, e.state.level)
        assertEquals("bong-bot", e.state.stageId)
        assertNull(e.state.branchId) // stage 2 chưa có nhánh
        assertTrue(events.any { it is GameEvent.Evolved &&
            it.fromStageId == "tan-lua" && it.toStageId == "bong-bot" && it.branchId == null })
    }

    @Test
    fun `nhánh Hiền được chọn khi streak đủ tại Lv16`() {
        val events = mutableListOf<GameEvent>()
        val e = engine(
            pet(level = 15, xp = Formulas.xpNeed(15) - 4, power = 30, streak = 8, stageId = "bong-bot"),
            events,
        )
        e.sessionComplete(5) // lên Lv.16

        assertEquals(16, e.state.level)
        assertEquals("diem-long", e.state.stageId)
        assertEquals("hien", e.state.branchId)
        assertTrue(events.any { it is GameEvent.Evolved && it.branchId == "hien" })
    }

    @Test
    fun `nhánh Chiến Binh được chọn khi sức mạnh ≥ 80 và streak thiếu`() {
        val e = engine(pet(level = 15, xp = Formulas.xpNeed(15) - 4, power = 85, streak = 2, stageId = "bong-bot"))
        e.sessionComplete(5)

        assertEquals("chien-binh", e.state.branchId)
    }

    @Test
    fun `chưa đủ điều kiện nhánh thì ra form thường (không branch)`() {
        val e = engine(pet(level = 15, xp = Formulas.xpNeed(15) - 4, power = 50, streak = 2, stageId = "bong-bot"))
        e.sessionComplete(5)

        assertEquals("diem-long", e.state.stageId)
        assertNull(e.state.branchId)
    }

    @Test
    fun `không tiến hoá khi level chưa tới mốc`() {
        val events = mutableListOf<GameEvent>()
        val e = engine(pet(level = 4, xp = 0), events)
        e.sessionComplete(25)

        assertEquals("tan-lua", e.state.stageId)
        assertTrue(events.filterIsInstance<GameEvent.Evolved>().isEmpty())
    }

    // ── 3. DECAY SAU NGÀY BỎ BÊ ───────────────────────────────────────

    @Test
    fun `ngày bỏ bê trừ 6 sức mạnh và 10 thể trạng, streak reset`() {
        val events = mutableListOf<GameEvent>()
        val e = engine(
            pet(power = 62, condition = 78, streak = 7, healthyDays = 3, lastDayEpochDay = 100L),
            events,
        )
        e.maybeAdvanceDay(101L) // DayEnd(101)

        assertEquals(56, e.state.power)   // −6
        assertEquals(68, e.state.condition) // −10
        assertEquals(0, e.state.streak)
        assertEquals(0, e.state.healthyDays)
        assertEquals(0, e.state.todaySessions)
        assertEquals(101L, e.state.lastDayEpochDay)
        assertEquals(1, e.state.history.size)
        assertEquals(false, e.state.history[0].goalMet)
        // phát StatusChange: 78 (Khỏe mạnh) → 68 (Bình thường)
        assertTrue(events.any { it is GameEvent.StatusChange &&
            it.old == ConditionBand.KHOE_MANH && it.new == ConditionBand.BINH_THUONG })
    }

    @Test
    fun `ngày đủ mục tiêu chỉ trừ 2 sức mạnh và cộng streak`() {
        val e = engine(
            pet(power = 62, condition = 78, streak = 6, todaySessions = 3, todayMinutes = 75,
                dayFed = true, dayHadSession = true, lastDayEpochDay = 100L),
        )
        e.maybeAdvanceDay(101L)

        assertEquals(60, e.state.power)  // −2 (đủ mục tiêu)
        assertEquals(78, e.state.condition) // có ăn + có phiên → không trừ
        assertEquals(7, e.state.streak)
        assertEquals(1, e.state.healthyDays)
        assertTrue(e.state.history[0].goalMet)
    }

    @Test
    fun `ngày có tập nhưng dưới mục tiêu thì không decay sức mạnh nhưng streak reset`() {
        val e = engine(pet(power = 62, condition = 78, streak = 5, todaySessions = 1, dayHadSession = true))
        e.maybeAdvanceDay(101L)

        assertEquals(62, e.state.power) // 0 decay
        assertEquals(78, e.state.condition) // có phiên (dù không ăn) → KHÔNG trừ −10 (spec: chỉ ngày không ăn & không phiên)
        assertEquals(0, e.state.streak)
    }

    @Test
    fun `nghỉ nhiều ngày thì decay cộng dồn theo từng ngày`() {
        val e = engine(pet(power = 62, condition = 78, lastDayEpochDay = 100L))
        e.maybeAdvanceDay(103L) // 3 ngày bị bỏ lỡ

        assertEquals(62 - 6 * 3, e.state.power)      // −18
        assertEquals(78 - 10 * 3, e.state.condition) // −30
        assertEquals(103L, e.state.lastDayEpochDay)
    }

    // ── 4. CHO ĂN HỒI PHỤC ────────────────────────────────────────────

    @Test
    fun `cho ăn hồi phục +15 thể trạng và đổi band ốm → thường`() {
        val events = mutableListOf<GameEvent>()
        val e = engine(pet(condition = 25, coins = 3), events) // 25 < 30 → Ốm yếu

        assertTrue(e.feed())
        assertEquals(40, e.state.condition)
        assertEquals(2, e.state.coins)
        assertEquals(ConditionBand.BINH_THUONG, e.state.conditionBand)
        assertTrue(events.any { it is GameEvent.StatusChange &&
            it.old == ConditionBand.OM_YEU && it.new == ConditionBand.BINH_THUONG })
    }

    @Test
    fun `không cho ăn được khi hết sushi`() {
        val e = engine(pet(condition = 25, coins = 0))

        assertFalse(e.feed())
        assertEquals(25, e.state.condition)
        assertEquals(0, e.state.coins)
    }

    // ── Sức mạnh: +8/phiên theo cap theo level ────────────────────────

    @Test
    fun `sức mạnh bị chặn tại cap theo level`() {
        // cap(4) = min(100, 40+24) = 64
        val e = engine(pet(level = 4, power = 60))
        e.sessionComplete(25)

        assertEquals(64, e.state.power)
    }

    @Test
    fun `mở khóa form lai khi đủ điều kiện kép`() {
        // Lv.12 cap 100 → power 85 hợp lệ; streak 10
        val events = mutableListOf<GameEvent>()
        val e = engine(pet(level = 12, power = 85, streak = 10), events)
        e.sessionComplete(25) // +8 power (93) — vẫn ≥ 85

        assertTrue("hoi-nuoc-boc" in e.state.unlockedFormIds)
        assertTrue(events.any { it is GameEvent.FormUnlocked && it.formId == "hoi-nuoc-boc" })
    }

    // ── Formulas & registry ───────────────────────────────────────────

    @Test
    fun `công thức spec §3 đúng số liệu`() {
        assertEquals(200, Formulas.xpNeed(4))
        assertEquals(64, Formulas.powerCap(4))
        assertEquals(88, Formulas.powerCap(8))
        assertEquals(100, Formulas.powerCap(20))
        assertEquals(ConditionBand.KHOE_MANH, ConditionBand.of(78))
        assertEquals(ConditionBand.BINH_THUONG, ConditionBand.of(68))
        assertEquals(ConditionBand.OM_YEU, ConditionBand.of(29))
    }

    @Test
    fun `registry điều kiện describe và evaluate`() {
        val s = pet(level = 16, power = 80, streak = 7)
        val cond = Condition.And(
            listOf(Condition.LevelAtLeast(16), Condition.StreakAtLeast(7)),
        )
        assertTrue(cond.evaluate(s))
        assertEquals("Lv ≥ 16 ∧ streak ≥ 7 ngày", cond.describe())
        assertFalse(cond.evaluate(s.copy(streak = 6)))
        val or = Condition.Or(listOf(Condition.PowerAtLeast(99), Condition.StreakAtLeast(7)))
        assertTrue(or.evaluate(s))
        assertEquals("Sức mạnh ≥ 99 ∨ streak ≥ 7 ngày", or.describe())
    }
}
