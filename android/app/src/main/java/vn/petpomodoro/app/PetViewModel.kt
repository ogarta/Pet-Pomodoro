package vn.petpomodoro.app

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import vn.petpomodoro.game.AlertSettings
import vn.petpomodoro.game.Backup
import vn.petpomodoro.game.Formulas
import vn.petpomodoro.game.GameEngine
import vn.petpomodoro.game.GameEvent
import vn.petpomodoro.game.GameState
import vn.petpomodoro.game.PetRepository
import vn.petpomodoro.game.SavedTimer

enum class TimerMode { IDLE, FOCUS, BREAK }

data class TimerUi(
    val mode: TimerMode = TimerMode.IDLE,
    val running: Boolean = false,
    val remainingMs: Long = Formulas.FOCUS_MINUTES * 60_000L,
    /** Tổng độ dài phiên lúc bắt đầu — bám phiên đang chạy như `totalMs` bên web. */
    val totalMs: Long = Formulas.FOCUS_MINUTES * 60_000L,
)

/**
 * PetViewModel — cầu nối UI ↔ GameEngine ↔ DataStore.
 * Timer là pomodoro THẬT, đếm bằng elapsedRealtime nên không trôi khi re-compose.
 *
 * Báo hết giờ (2 đường, không trùng nhau):
 *  - Process sống: ticker chạm 0 → VM tự hoàn tất phiên + chuông (Sfx) + notification,
 *    rồi huỷ alarm.
 *  - Process chết: alarm vẫn hẹn → TimerReceiver đọc prefs (DataStore sống qua death)
 *    → chuông + notification; lần mở app sau, [restoreSavedTimer] vẫn tính phiên.
 */
class PetViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = PetRepository(app)
    private var engine: GameEngine? = null

    private val _state = MutableStateFlow<GameState?>(null)
    val state: StateFlow<GameState?> = _state.asStateFlow()

    private val _timer = MutableStateFlow(TimerUi())
    val timer: StateFlow<TimerUi> = _timer.asStateFlow()

    private val _alerts = MutableStateFlow(AlertSettings())
    val alerts: StateFlow<AlertSettings> = _alerts.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    private var ticker: Job? = null
    private var endAtRealtime = 0L
    private val sfx by lazy { Sfx(app) }

    init {
        viewModelScope.launch {
            _alerts.value = repo.loadSettings()
            val saved = repo.load()
            if (saved != null) {
                attach(GameEngine(saved))
                advanceDay() // đóng các ngày bỏ lỡ (decay §3) ngay khi mở app
                restoreSavedTimer(repo.loadTimer())
            } else {
                _state.value = null
            }
        }
    }

    private fun attach(e: GameEngine) {
        e.addListener { ev ->
            _events.tryEmit(ev)
            _state.value = e.state
            persist()
        }
        engine = e
        _state.value = e.state
    }

    private fun persist() {
        val e = engine ?: return
        viewModelScope.launch { repo.save(e.state) }
    }

    /** Nhận trứng (lần đầu) hoặc chọn lại trứng (reset, có confirm ở UI). */
    fun adopt(lineId: String) {
        abortTimer()
        attach(GameEngine(GameEngine.adopt(lineId, LocalDate.now().toEpochDay())))
        persist()
    }

    /** Gọi mỗi khi app ON_RESUME: tự đóng các ngày bỏ lỡ bằng sự kiện DayEnd. */
    fun advanceDay() {
        engine?.maybeAdvanceDay(LocalDate.now().toEpochDay())
    }

    fun feed(): Boolean {
        val ok = engine?.feed() ?: false
        if (ok && _alerts.value.sound) sfx.play(Sfx.Kind.FEED)
        return ok
    }

    /** Fanfare tiến hoá — MainActivity gọi khi nhận sự kiện Evolved. */
    fun playEvolveFanfare() {
        if (_alerts.value.sound) sfx.play(Sfx.Kind.EVOLVE)
    }

    // ── Tuỳ chọn cảnh báo ─────────────────────────────────────────────

    fun toggleSound() = setAlerts(_alerts.value.copy(sound = !_alerts.value.sound))

    fun toggleNotify() = setAlerts(_alerts.value.copy(notify = !_alerts.value.notify))

    private fun setAlerts(a: AlertSettings) {
        _alerts.value = a
        viewModelScope.launch { repo.saveSettings(a) }
    }

    // ── Pomodoro ──────────────────────────────────────────────────────

    fun startFocus() {
        val t = _timer.value
        if (t.mode == TimerMode.IDLE && !t.running) {
            beginCountdown(Formulas.FOCUS_MINUTES * 60_000L, TimerMode.FOCUS)
        }
    }

    fun pauseTimer() {
        if (!_timer.value.running) return
        ticker?.cancel()
        val remaining = (endAtRealtime - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        val paused = _timer.value.copy(running = false, remainingMs = remaining)
        _timer.value = paused
        TimerAlarms.cancel(getApplication())
        saveTimerPrefs(paused)
    }

    fun resumeTimer() {
        val t = _timer.value
        if (t.running || t.mode == TimerMode.IDLE) return
        beginCountdown(t.remainingMs, t.mode)
    }

    fun abortTimer() {
        ticker?.cancel()
        TimerAlarms.cancel(getApplication())
        saveTimerPrefs(null)
        _timer.value = TimerUi()
    }

    private fun beginCountdown(durationMs: Long, mode: TimerMode) {
        endAtRealtime = SystemClock.elapsedRealtime() + durationMs
        val t = TimerUi(mode, running = true, remainingMs = durationMs, totalMs = durationMs)
        _timer.value = t
        saveTimerPrefs(t)
        TimerAlarms.schedule(getApplication(), System.currentTimeMillis() + durationMs)
        startTicker()
    }

    /** Khôi phục countdown sau process death — prefs là nguồn chân lý (web: restoreTimer). */
    private fun restoreSavedTimer(t: SavedTimer?) {
        if (t == null) return
        val mode = when (t.phase) {
            "focus" -> TimerMode.FOCUS
            "break" -> TimerMode.BREAK
            else -> return
        }
        if (!t.running) {
            // đang pause lúc process chết → giữ nguyên mốc remaining
            _timer.value = TimerUi(mode, running = false, remainingMs = t.remainingMs, totalMs = t.totalMs)
            return
        }
        val remaining = t.endAtWallMs - System.currentTimeMillis()
        if (remaining > 0) {
            endAtRealtime = SystemClock.elapsedRealtime() + remaining
            _timer.value = TimerUi(mode, running = true, remainingMs = remaining, totalMs = t.totalMs)
            startTicker() // alarm vẫn hẹn sẵn từ trước — không đặt lại
        } else {
            // hết giờ lúc vắng mặt → VẪN TÍNH PHIÊN (đúng semantics bên web)
            if (mode == TimerMode.FOCUS) {
                engine?.sessionComplete((t.totalMs / 60_000L).toInt().coerceAtLeast(1), -1)
            }
            abortTimer()
        }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (isActive) {
                val remaining = endAtRealtime - SystemClock.elapsedRealtime()
                if (remaining <= 0) break
                _timer.value = _timer.value.copy(remainingMs = remaining)
                delay(250)
            }
            onCountdownEnd()
        }
    }

    private fun onCountdownEnd() {
        when (_timer.value.mode) {
            TimerMode.FOCUS -> {
                val now = LocalTime.now()
                engine?.sessionComplete(Formulas.FOCUS_MINUTES, now.hour * 60 + now.minute)
                if (_alerts.value.sound) sfx.play(Sfx.Kind.FOCUS_DONE)
                Notifications.showTimerEnd(getApplication(), focusDone = true, minutes = Formulas.FOCUS_MINUTES)
                TimerAlarms.cancel(getApplication()) // VM đã lo báo — receiver khỏi báo nữa
                // nghỉ 5 phút tự động
                beginCountdown(Formulas.BREAK_MINUTES * 60_000L, TimerMode.BREAK)
            }
            else -> {
                if (_alerts.value.sound) sfx.play(Sfx.Kind.BREAK_DONE)
                Notifications.showTimerEnd(getApplication(), focusDone = false, minutes = Formulas.BREAK_MINUTES)
                abortTimer()
            }
        }
    }

    private fun saveTimerPrefs(t: TimerUi?) {
        viewModelScope.launch {
            repo.saveTimer(
                when {
                    t == null || t.mode == TimerMode.IDLE -> null
                    else -> SavedTimer(
                        phase = if (t.mode == TimerMode.FOCUS) "focus" else "break",
                        running = t.running,
                        endAtWallMs = System.currentTimeMillis() + t.remainingMs,
                        remainingMs = t.remainingMs,
                        totalMs = t.totalMs,
                    )
                },
            )
        }
    }

    // ── Bản lưu pet (xuất/nhập JSON dùng chéo với web) ─────────────────

    fun exportBackup(): String? {
        val s = engine?.state ?: return null
        val savedTimer = _timer.value.takeIf { it.mode != TimerMode.IDLE }?.let { t ->
            SavedTimer(
                phase = if (t.mode == TimerMode.FOCUS) "focus" else "break",
                running = t.running,
                endAtWallMs = System.currentTimeMillis() + t.remainingMs,
                remainingMs = t.remainingMs,
                totalMs = t.totalMs,
            )
        }
        return Backup.exportJson(s, _alerts.value, savedTimer)
    }

    fun writeBackup(uri: Uri): Boolean {
        val json = exportBackup() ?: return false
        return try {
            getApplication<Application>().contentResolver
                .openOutputStream(uri)?.use { it.write(json.toByteArray()) } != null
        } catch (_: Exception) {
            false
        }
    }

    /** null = đọc/parse thất bại; false = file hợp lệ nhưng ghi chưa xong. */
    fun readBackup(uri: Uri): Boolean? {
        val text = try {
            getApplication<Application>().contentResolver
                .openInputStream(uri)?.use { it.readBytes().decodeToString() }
        } catch (_: Exception) {
            null
        } ?: return null
        val imported = Backup.parseJson(text) ?: return null
        abortTimer() // phiên đang chạy nhường chỗ cho bản lưu
        attach(GameEngine(imported.state))
        viewModelScope.launch {
            repo.saveSettings(imported.alerts)
            _alerts.value = imported.alerts
            advanceDay()
            persist()
        }
        return true
    }

    override fun onCleared() {
        ticker?.cancel()
        super.onCleared()
    }
}
