package vn.petpomodoro.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/**
 * TimerAlarms — hẹn AlarmManager báo hết giờ pomodoro khi app ở nền.
 * Dùng `setAndAllowWhileIdle` (RTC_WAKEUP): không cần quyền SCHEDULE_EXACT_ALARM,
 * chênh lệch vài giây là chấp nhận được với pomodoro.
 */
object TimerAlarms {

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            1001,
            Intent(context, TimerReceiver::class.java).setAction(TimerReceiver.ACTION_TIMER_END),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun schedule(context: Context, endAtWallMs: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAtWallMs, pendingIntent(context))
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(context))
    }
}
