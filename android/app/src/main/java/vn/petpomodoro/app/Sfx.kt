package vn.petpomodoro.app

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

/** Sfx — chuông ngắn trong app (SoundPool). Âm thanh nền khi app đóng dùng MediaPlayer trong TimerReceiver. */
class Sfx(context: Context) {

    enum class Kind { FOCUS_DONE, BREAK_DONE, FEED, EVOLVE }

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val ids: Map<Kind, Int> = mapOf(
        Kind.FOCUS_DONE to pool.load(context, R.raw.chime_focus, 1),
        Kind.BREAK_DONE to pool.load(context, R.raw.chime_break, 1),
        Kind.FEED to pool.load(context, R.raw.pop_feed, 1),
        Kind.EVOLVE to pool.load(context, R.raw.fanfare_evolve, 1),
    )

    fun play(kind: Kind) {
        val id = ids[kind] ?: return
        pool.play(id, 0.9f, 0.9f, 1, 0, 1f)
    }
}
