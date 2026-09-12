package vn.petpomodoro.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/** Notifications — kênh + helper bắn thông báo hết giờ (chỉ dùng từ TimerReceiver). */
object Notifications {

    const val CHANNEL_TIMER = "timer"
    private const val ID_TIMER_END = 2001

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_TIMER,
            "Báo hết giờ Pomodoro",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = "Chuông + thông báo khi phiên tập hoặc giờ nghỉ kết thúc" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun showTimerEnd(context: Context, focusDone: Boolean, minutes: Int) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val tapIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_TIMER)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(if (focusDone) "Bé xong phiên rồi! 🎉" else "Hết giờ nghỉ! ☕")
            .setContentText(
                if (focusDone) "$minutes phút tập hoàn thành — giờ nghỉ một chút nhé!"
                else "Tỉnh dậy nào — quay lại tập tiếp thôi!",
            )
            .setAutoCancel(true)
            .setContentIntent(tapIntent)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(ID_TIMER_END, notif)
        } catch (_: SecurityException) {
            // chưa được cấp quyền POST_NOTIFICATIONS — bỏ qua
        }
    }
}
