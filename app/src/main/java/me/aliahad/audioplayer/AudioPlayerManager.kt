package me.aliahad.audioplayer

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession

class AudioPlayerManager private constructor(private val appContext: Context) {

    val player: ExoPlayer = ExoPlayer.Builder(appContext).build()
    val mediaSession: MediaSession = MediaSession.Builder(appContext, player).build()

    fun release() {
        mediaSession.release()
        player.release()
    }

    companion object {
        @Volatile
        private var instance: AudioPlayerManager? = null

        fun getInstance(context: Context): AudioPlayerManager {
            return instance ?: synchronized(this) {
                instance ?: AudioPlayerManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
