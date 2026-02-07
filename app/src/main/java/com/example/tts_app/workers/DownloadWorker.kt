package com.example.tts_app.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.tts_app.data.TtsRepository
import com.example.tts_app.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class DownloadWorker(
    context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelId = "download_channel"
    private val notificationId = 101

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val novelId = inputData.getInt("novelId", -1)
        val limit = inputData.getInt("limit", -1)
        val mode = inputData.getString("mode") ?: "all"

        if (novelId == -1) return@withContext Result.failure()

        createNotificationChannel()

        setForeground(createForegroundInfo(0, 0, "Starting download..."))

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = TtsRepository(applicationContext, database.bookDao())
        val dao = database.bookDao()

        val novel = dao.getNovelById(novelId) ?: return@withContext Result.failure()
        val allChapters = dao.getChapterList(novelId)

        val startIndex = if (mode == "unread") novel.currentChapterIndex else 0
        val candidateChapters = allChapters.filter { it.index >= startIndex && !it.isDownloaded }
        val targetChapters = if (limit == -1) candidateChapters else candidateChapters.take(limit)

        if (targetChapters.isEmpty()) return@withContext Result.success()

        val total = targetChapters.size
        var current = 0
        var lastUpdateTime = 0L

        for (chapter in targetChapters) {
            if (isStopped) {
                updateNotification(current, total, "Download Cancelled", false)
                return@withContext Result.success()
            }

            try {
                repository.downloadChapterExplicitly(chapter.id)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            current++

            val currentTime = System.currentTimeMillis()
            if (currentTime - lastUpdateTime > 1000 || current == total) {
                setProgress(workDataOf("progress" to (current.toFloat() / total.toFloat())))
                setForeground(createForegroundInfo(current, total, novel.title))
                lastUpdateTime = currentTime
            }

            delay(250)
        }

        return@withContext Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Downloads", NotificationManager.IMPORTANCE_LOW)
            channel.description = "Background download progress"
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundInfo(current: Int, total: Int, title: String): ForegroundInfo {
        val cancelIntent = WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(id)

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Downloading: $title")
            .setContentText(if (total > 0) "Chapter $current of $total" else "Starting...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(total, current, false)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ForegroundInfo(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            return ForegroundInfo(notificationId, notification)
        }
    }

    private fun updateNotification(current: Int, total: Int, title: String, ongoing: Boolean) {
        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setContentText(if (ongoing) "Chapter $current of $total" else "Download stopped")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(false)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
    }
}