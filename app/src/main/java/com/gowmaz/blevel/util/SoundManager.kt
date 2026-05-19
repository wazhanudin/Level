package com.gowmaz.blevel.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import com.gowmaz.blevel.R

/**
 * Manages sound effects for the application.
 */
class SoundManager(private val context: Context) {

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()
        )
        .build()

    private val bipSoundID: Int = soundPool.load(context, R.raw.bip, 1)
    private val bipRate: Int = try {
        context.resources.getInteger(R.integer.bip_rate).coerceAtLeast(100)
    } catch (e: Exception) {
        200 // Default fallback
    }
    private var lastBip: Long = 0

    fun playBip() {
        if (System.currentTimeMillis() - lastBip > bipRate) {
            val mgr = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (mgr != null) {
                val volume = try {
                    val streamVolumeCurrent = mgr.getStreamVolume(AudioManager.STREAM_RING).toFloat()
                    val streamVolumeMax = mgr.getStreamMaxVolume(AudioManager.STREAM_RING).toFloat()
                    if (streamVolumeMax > 0) streamVolumeCurrent / streamVolumeMax else 0.5f
                } catch (e: Exception) {
                    0.5f // Default volume on error
                }
                lastBip = System.currentTimeMillis()
                soundPool.play(bipSoundID, volume, volume, 1, 0, 1f)
            }
        }
    }

    fun release() {
        soundPool.release()
    }
}
