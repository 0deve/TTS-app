package com.example.tts_app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.tts_app.MainActivity

class TtsService : Service() {

    private val CHANNEL_ID = "tts_playback_channel"
    private val NOTIFICATION_ID = 202
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TtsApp:PlaybackLock")

        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "TtsApp:WifiLock")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        val action = intent.action
        if (action == "STOP_SERVICE") {
            stopForeground(true)
            stopSelf()
            wakeLock?.let { if (it.isHeld) it.release() }
            wifiLock?.let { if (it.isHeld) it.release() }
            return START_NOT_STICKY
        }

        val title = intent.getStringExtra("title") ?: "Audio Book"
        val chapter = intent.getStringExtra("chapter") ?: ""
        val isPlaying = intent.getBooleanExtra("isPlaying", false)

        if (isPlaying) {
            wakeLock?.let { if (!it.isHeld) it.acquire(30 * 60 * 1000L) }
            wifiLock?.let { if (!it.isHeld) it.acquire() }
        } else {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    it.acquire(10 * 60 * 1000L)
                }
            }
            wifiLock?.let { if (it.isHeld) it.release() }
        }

        val notification = buildNotification(title, chapter, isPlaying)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        return START_STICKY
    }

    private fun buildNotification(title: String, chapter: String, isPlaying: Boolean): android.app.Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        openIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        val openPendingIntent = PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE)

        val playPauseAction = if (isPlaying) "ACTION_PAUSE" else "ACTION_PLAY"
        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseLabel = if (isPlaying) "Pause" else "Play"

        val playPauseIntentRaw = Intent(playPauseAction).setPackage(packageName)
        val playPauseIntent = PendingIntent.getBroadcast(this, 1, playPauseIntentRaw, PendingIntent.FLAG_IMMUTABLE)

        val prevIntentRaw = Intent("ACTION_PREVIOUS").setPackage(packageName)
        val prevIntent = PendingIntent.getBroadcast(this, 2, prevIntentRaw, PendingIntent.FLAG_IMMUTABLE)

        val nextIntentRaw = Intent("ACTION_NEXT").setPackage(packageName)
        val nextIntent = PendingIntent.getBroadcast(this, 3, nextIntentRaw, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(chapter)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(openPendingIntent)
            .setOngoing(isPlaying)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2))
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevIntent)
            .addAction(playPauseIcon, playPauseLabel, playPauseIntent)
            .addAction(android.R.drawable.ic_media_next, "Next", nextIntent)
            .setOnlyAlertOnce(true)

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "TTS Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Controls for TTS playback"
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        wakeLock?.let { if (it.isHeld) it.release() }
        wifiLock?.let { if (it.isHeld) it.release() }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}