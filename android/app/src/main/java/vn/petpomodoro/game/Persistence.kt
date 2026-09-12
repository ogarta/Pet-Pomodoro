package vn.petpomodoro.game

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.petDataStore by preferencesDataStore(name = "petpomodoro")

/** Tuỳ chọn cảnh báo (khớp `state/meta.alerts` bên web) — mặc định bật cả hai. */
data class AlertSettings(
    val sound: Boolean = true,
    val notify: Boolean = true,
)

/**
 * Snapshot timer pomodoro đang chạy — lưu DataStore để (1) khôi phục phiên qua process death
 * (ngang hàng `restoreTimer` bên web) và (2) TimerReceiver đối chiếu trước khi báo.
 * [endAtWallMs] là giờ tường `System.currentTimeMillis()` vì AlarmManager hẹn theo RTC.
 */
data class SavedTimer(
    val phase: String, // "focus" | "break"
    val running: Boolean,
    val endAtWallMs: Long,
    val remainingMs: Long,
    val totalMs: Long,
)

/**
 * Persistence.kt — lưu cục bộ bằng DataStore Preferences, OFFLINE hoàn toàn.
 *
 * Tên key CHẢY THEO SHAPE FIRESTORE TƯƠNG LAI (spec §1.5) để M4 chỉ cần cắm sync:
 *   `users/{uid}/state/pet`            → key "state/pet.<field>"           (core §1.1)
 *   `users/{uid}/systems/{systemId}`   → key "systems/<system>.<field>"    (mỗi hệ 1 "doc")
 * Thêm hệ mới = thêm key "systems/<he>…" — không đụng key hệ cũ.
 * "stats/history.*" là phần mở rộng cục bộ (lịch sử ngày/phiên cho màn Stats).
 */
class PetRepository(private val context: Context) {

    private object K {
        // users/{uid}/state/pet — core (spec §1.1)
        val speciesId = stringPreferencesKey("state/pet.speciesId")
        val level = intPreferencesKey("state/pet.level")
        val xp = intPreferencesKey("state/pet.xp")
        val condition = intPreferencesKey("state/pet.condition")
        val coins = intPreferencesKey("state/pet.coins")
        val streak = intPreferencesKey("state/pet.streak")
        val todaySessions = intPreferencesKey("state/pet.todaySessions")
        val todayMinutes = intPreferencesKey("state/pet.todayMinutes")
        val todayXp = intPreferencesKey("state/pet.todayXp")
        val dayFed = booleanPreferencesKey("state/pet.dayFed")
        val dayHadSession = booleanPreferencesKey("state/pet.dayHadSession")
        val lastDayEpochDay = longPreferencesKey("state/pet.lastDayEpochDay")

        // users/{uid}/systems/power
        val power = intPreferencesKey("systems/power.power")
        val healthyDays = intPreferencesKey("systems/power.healthyDays")

        // users/{uid}/systems/evolution
        val stageId = stringPreferencesKey("systems/evolution.stageId")
        val branchId = stringPreferencesKey("systems/evolution.branchId")
        val unlockedFormIds = stringSetPreferencesKey("systems/evolution.unlockedFormIds")

        // phần mở rộng cục bộ — lịch sử
        val historyDays = stringPreferencesKey("stats/history.days")
        val sessionLog = stringPreferencesKey("stats/history.sessions")

        // settings — tuỳ chọn cảnh báo (web: state/meta → alerts)
        val settingSound = booleanPreferencesKey("settings.sound")
        val settingNotify = booleanPreferencesKey("settings.notify")

        // snapshot timer đang chạy (khôi phục qua process death + đối chiếu alarm)
        val timerPhase = stringPreferencesKey("state/timer.phase")
        val timerRunning = booleanPreferencesKey("state/timer.running")
        val timerEndAtMs = longPreferencesKey("state/timer.endAtMs")
        val timerRemainingMs = longPreferencesKey("state/timer.remainingMs")
        val timerTotalMs = longPreferencesKey("state/timer.totalMs")
    }

    suspend fun save(state: GameState) {
        context.petDataStore.edit { p ->
            p[K.speciesId] = state.speciesId
            p[K.level] = state.level
            p[K.xp] = state.xp
            p[K.condition] = state.condition
            p[K.coins] = state.coins
            p[K.streak] = state.streak
            p[K.todaySessions] = state.todaySessions
            p[K.todayMinutes] = state.todayMinutes
            p[K.todayXp] = state.todayXp
            p[K.dayFed] = state.dayFed
            p[K.dayHadSession] = state.dayHadSession
            p[K.lastDayEpochDay] = state.lastDayEpochDay
            p[K.power] = state.power
            p[K.healthyDays] = state.healthyDays
            p[K.stageId] = state.stageId
            val branch = state.branchId
            if (branch != null) p[K.branchId] = branch else p.remove(K.branchId)
            p[K.unlockedFormIds] = state.unlockedFormIds
            p[K.historyDays] = state.history.joinToString(SEP_REC) {
                listOf(it.epochDay, it.minutes, it.sessions, if (it.goalMet) 1 else 0).joinToString(SEP_FIELD)
            }
            p[K.sessionLog] = state.sessionLog.joinToString(SEP_REC) {
                listOf(it.epochDay, it.minuteOfDay, it.minutes, it.xp).joinToString(SEP_FIELD)
            }
        }
    }

    /** null = chưa nhận trứng (mở app lần đầu) */
    suspend fun load(): GameState? = decode(context.petDataStore.data.first())

    fun observe(): Flow<GameState?> = context.petDataStore.data.map { decode(it) }

    private fun decode(p: androidx.datastore.preferences.core.Preferences): GameState? {
        val speciesId = p[K.speciesId] ?: return null
        return GameState(
            speciesId = speciesId,
            stageId = p[K.stageId] ?: Catalog.line(speciesId).stages.first().id,
            branchId = p[K.branchId],
            level = p[K.level] ?: 1,
            xp = p[K.xp] ?: 0,
            power = p[K.power] ?: 40,
            condition = p[K.condition] ?: 80,
            coins = p[K.coins] ?: Formulas.SUSHI_START,
            streak = p[K.streak] ?: 0,
            healthyDays = p[K.healthyDays] ?: 0,
            todaySessions = p[K.todaySessions] ?: 0,
            todayMinutes = p[K.todayMinutes] ?: 0,
            todayXp = p[K.todayXp] ?: 0,
            dayFed = p[K.dayFed] ?: false,
            dayHadSession = p[K.dayHadSession] ?: false,
            lastDayEpochDay = p[K.lastDayEpochDay] ?: 0L,
            unlockedFormIds = p[K.unlockedFormIds] ?: emptySet(),
            history = p[K.historyDays]?.takeIf { it.isNotEmpty() }?.split(SEP_REC)?.map { row ->
                val c = row.split(SEP_FIELD)
                DayRecord(c[0].toLong(), c[1].toInt(), c[2].toInt(), c[3] == "1")
            } ?: emptyList(),
            sessionLog = p[K.sessionLog]?.takeIf { it.isNotEmpty() }?.split(SEP_REC)?.map { row ->
                val c = row.split(SEP_FIELD)
                SessionLogEntry(c[0].toLong(), c[1].toInt(), c[2].toInt(), c[3].toInt())
            } ?: emptyList(),
        )
    }

    companion object {
        private const val SEP_REC = ";"
        private const val SEP_FIELD = ","
    }

    /** Tuỳ chọn cảnh báo — mặc định bật cả hai nếu chưa lưu. */
    suspend fun saveSettings(s: AlertSettings) {
        context.petDataStore.edit { p ->
            p[K.settingSound] = s.sound
            p[K.settingNotify] = s.notify
        }
    }

    suspend fun loadSettings(): AlertSettings {
        val p = context.petDataStore.data.first()
        return AlertSettings(sound = p[K.settingSound] ?: true, notify = p[K.settingNotify] ?: true)
    }

    /** null = xoá snapshot (timer idle/đã hủy). */
    suspend fun saveTimer(t: SavedTimer?) {
        context.petDataStore.edit { p ->
            if (t == null) {
                p.remove(K.timerPhase)
                p.remove(K.timerRunning)
                p.remove(K.timerEndAtMs)
                p.remove(K.timerRemainingMs)
                p.remove(K.timerTotalMs)
            } else {
                p[K.timerPhase] = t.phase
                p[K.timerRunning] = t.running
                p[K.timerEndAtMs] = t.endAtWallMs
                p[K.timerRemainingMs] = t.remainingMs
                p[K.timerTotalMs] = t.totalMs
            }
        }
    }

    suspend fun loadTimer(): SavedTimer? {
        val p = context.petDataStore.data.first()
        val phase = p[K.timerPhase] ?: return null
        return SavedTimer(
            phase = phase,
            running = p[K.timerRunning] ?: false,
            endAtWallMs = p[K.timerEndAtMs] ?: 0L,
            remainingMs = p[K.timerRemainingMs] ?: 0L,
            totalMs = p[K.timerTotalMs] ?: 0L,
        )
    }
}
