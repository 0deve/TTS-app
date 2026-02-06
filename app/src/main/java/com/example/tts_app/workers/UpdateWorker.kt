package com.example.tts_app.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.tts_app.data.TtsRepository
import com.example.tts_app.data.local.AppDatabase

class UpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = TtsRepository(applicationContext, database.bookDao())
        val dao = database.bookDao()

        val novels = dao.getAllNovelsSync().filter { it.inLibrary }

        novels.forEach { novel ->
            try {
                val added = repository.loadMoreChapters(novel.id)
                if (added > 0) {
                    dao.updateNovel(novel.copy(hasUnseenUpdates = true, totalChapters = novel.totalChapters + added))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return Result.success()
    }
}