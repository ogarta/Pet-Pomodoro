package vn.petpomodoro.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import vn.petpomodoro.game.PetRepository

/**
 * TimerReceiver — alarm hết giờ pomodoro bắn vào đây (cả khi app ở nền hoặc
 * process đã chết, vì DataStore prefs vẫn còn). Nhiệm vụ: chuông + thông báo.
 * KHÔNG đụng game state — phiên hoàn thành do PetViewModel lo (ticker nếu còn
 * sống, hoặc đường khôi phục khi mở app nếu process đã chết).
 */
class TimerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TIMER_END) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = PetRepository(context)
                val t = repo.loadTimer()
                // đối chiếu snapshot: chỉ báo khi timer thật sự đang chạy & đúng hẹn
                // (user đã hủy/pause thì prefs bị xoá/sửa → không báo oan)
                if (t?.running == true && t.endAtWallMs in 1..(System.currentTimeMillis() + 5000)) {
                    val focusDone = t.phase == "focus"
                    if (repo.loadSettings().sound) {
                        playChime(context, if (focusDone) R.raw.chime_focus else R.raw.chime_break)
                    }
                    Notifications.showTimerEnd(context, focusDone, (t.totalMs / 60_000L).toInt().coerceAtLeast(1))
                }
            } catch (_: Exception) {
                // báo thức là phụ — không để vỡ
            } finally {
                pending.finish()
            }
        }
    }

    private fun playChime(context: Context, res: Int) {
        try {
            MediaPlayer.create(context, res)?.apply {
                setOnCompletionListener { release() }
                start()
            }
        } catch (_: Exception) {
        }
    }

    companion object {
        const val ACTION_TIMER_END = "vn.petpomodoro.app.ACTION_TIMER_END"
    }
}
