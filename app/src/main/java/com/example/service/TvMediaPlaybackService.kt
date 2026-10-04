package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

@OptIn(UnstableApi::class)
class TvMediaPlaybackService : MediaSessionService() {

    companion object {
        const val CHANNEL_ID = "vma_media_playback"
        const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        try {
            val provider = DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(CHANNEL_ID)
                .setNotificationId(NOTIFICATION_ID)
                .build()
            provider.setSmallIcon(com.example.R.drawable.ic_notification_reminder)
            setMediaNotificationProvider(provider)
        } catch (e: Throwable) {
            android.util.Log.e("TvPlaybackService", "Error setting media notification provider", e)
        }

        TvPlayerManager.onServiceCreated(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        TvPlayerManager.mediaSession?.let { session ->
            registerSession(session)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Phát truyền hình VMA Live",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Điều khiển đa phương tiện & thông tin kênh phát sóng VMA Live"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun registerSession(session: MediaSession) {
        try {
            addSession(session)
        } catch (_: Exception) {}
    }

    fun unregisterSession(session: MediaSession) {
        try {
            removeSession(session)
        } catch (_: Exception) {}
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        val session = TvPlayerManager.mediaSession ?: run {
            TvPlayerManager.getOrCreatePlayer(applicationContext)
            TvPlayerManager.mediaSession
        }
        session?.let { registerSession(it) }
        return session
    }

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        try {
            super.onUpdateNotification(session, startInForegroundRequired)
        } catch (t: Throwable) {
            android.util.Log.e("TvPlaybackService", "Error in onUpdateNotification: ${t.message}", t)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val player = TvPlayerManager.exoPlayer
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        TvPlayerManager.onServiceDestroyed()
        super.onDestroy()
    }
}
