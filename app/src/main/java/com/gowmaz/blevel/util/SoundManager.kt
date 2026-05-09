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
    private val bipRate: Int = context.resources.getInteger(R.integer.bip_rate)
    private var lastBip: Long = 0

    fun playBip() {
        if (System.currentTimeMillis() - lastBip > bipRate) {
            val mgr = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (mgr != null) {
                val streamVolumeCurrent = mgr.getStreamVolume(AudioManager.STREAM_RING).toFloat()
                val streamVolumeMax = mgr.getStreamMaxVolume(AudioManager.STREAM_RING).toFloat()
                val volume = streamVolumeCurrent / streamVolumeMax
                lastBip = System.currentTimeMillis()
                soundPool.play(bipSoundID, volume, volume, 1, 0, 1f)
            }
        }
    }

    fun release() {
        soundPool.release()
    }
}
