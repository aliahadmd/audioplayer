package me.aliahad.audioplayer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerNotificationManager

class AudioPlayerService : android.app.Service() {

    private lateinit var playerNotificationManager: PlayerNotificationManager
    private lateinit var mediaSession: MediaSession
    private lateinit var player: Player
    private var isForeground = false

    override fun onCreate() {
        super.onCreate()
        val manager = AudioPlayerManager.getInstance(this)
        mediaSession = manager.mediaSession
        player = manager.player
        createNotificationChannel()
        playerNotificationManager = buildNotificationManager()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())
        isForeground = true
        playerNotificationManager.setMediaSessionToken(mediaSession.sessionCompatToken)
        playerNotificationManager.setPlayer(player)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP && !player.playWhenReady) {
            stopForegroundService()
        } else {
            playerNotificationManager.invalidate()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        playerNotificationManager.setPlayer(null)
        stopForeground(true)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotificationManager(): PlayerNotificationManager {
        val mediaDescriptionAdapter = object : PlayerNotificationManager.MediaDescriptionAdapter {
            override fun getCurrentContentTitle(player: Player): CharSequence {
                return player.mediaMetadata.title ?: "Unknown"
            }

            override fun createCurrentContentIntent(player: Player): PendingIntent? {
                val intent = Intent(this@AudioPlayerService, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                return PendingIntent.getActivity(
                    this@AudioPlayerService,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }

            override fun getCurrentContentText(player: Player): CharSequence? {
                val artist = player.mediaMetadata.artist
                val album = player.mediaMetadata.albumTitle
                return when {
                    !artist.isNullOrBlank() && !album.isNullOrBlank() -> "$artist • $album"
                    !artist.isNullOrBlank() -> artist
                    !album.isNullOrBlank() -> album
                    else -> null
                }
            }

            override fun getCurrentLargeIcon(player: Player, callback: PlayerNotificationManager.BitmapCallback): android.graphics.Bitmap? = null
        }

        val notificationListener = object : PlayerNotificationManager.NotificationListener {
            override fun onNotificationPosted(
                notificationId: Int,
                notification: Notification,
                ongoing: Boolean
            ) {
                if (ongoing && !isForeground) {
                    startForeground(notificationId, notification)
                    isForeground = true
                } else if (!ongoing && isForeground) {
                    stopForeground(STOP_FOREGROUND_DETACH)
                    isForeground = false
                }
            }

            override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
                if (isForeground) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    isForeground = false
                }
                if (!player.playWhenReady || player.playbackState == Player.STATE_IDLE) {
                    stopSelf()
                }
            }
        }

        val builder = PlayerNotificationManager.Builder(
            this,
            NOTIFICATION_ID,
            NOTIFICATION_CHANNEL_ID
        )
            .setMediaDescriptionAdapter(mediaDescriptionAdapter)
            .setNotificationListener(notificationListener)
        return builder.build().apply {
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setUseRewindAction(false)
            setUseFastForwardAction(false)
            setPriority(NotificationCompat.PRIORITY_HIGH)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = player.mediaMetadata.title ?: getString(R.string.app_name)
        val subtitle = player.mediaMetadata.artist ?: player.mediaMetadata.albumTitle

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun stopForegroundService() {
        playerNotificationManager.setPlayer(null)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_CHANNEL_ID = "audio_playback_channel"
        private const val ACTION_STOP = "me.aliahad.audioplayer.action.STOP"

        fun startService(context: Context) {
            val intent = Intent(context, AudioPlayerService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AudioPlayerService::class.java).apply { action = ACTION_STOP }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
