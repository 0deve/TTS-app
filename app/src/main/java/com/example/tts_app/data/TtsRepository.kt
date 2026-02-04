package com.example.tts_app.data
import android.content.Context
import android.util.Log
import com.example.tts_app.api.AllTalkApi
import com.example.tts_app.data.local.BookDao
import com.example.tts_app.data.local.Chapter
import com.example.tts_app.data.local.Novel
import com.example.tts_app.data.remote.NovelSource
import com.example.tts_app.data.remote.SourceFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class TtsRepository(
    context: Context,
    private val bookDao: BookDao
) {
    private var api: AllTalkApi? = null
    private val cacheDir = context.cacheDir
    private var currentBaseUrl: String = "http://127.0.0.1:8774"

    private val scraper: NovelSource = SourceFactory.getSource()

    init {
        buildClient(currentBaseUrl)
    }

    suspend fun searchRemoteNovels(query: String): List<Novel> {
        return scraper.searchNovels(query)
    }

    fun getDownloadedNovels(): Flow<List<Novel>> {
        return bookDao.getDownloadedNovels()
    }

    suspend fun addToLibrary(novel: Novel) {
        val (metadata, firstBatch) = scraper.getNovelDetails(novel.url)

        val existing = bookDao.getNovelByUrl(novel.url)
        val novelId = if (existing != null) {
            val updated = existing.copy(
                inLibrary = true,
                coverUrl = if (metadata.coverUrl.isNotEmpty()) metadata.coverUrl else existing.coverUrl,
                author = if (metadata.author.isNotEmpty()) metadata.author else existing.author,
                summary = if (metadata.summary.isNotEmpty()) metadata.summary else existing.summary,
                totalChapters = if (metadata.totalChapters > 0) metadata.totalChapters else firstBatch.size
            )
            bookDao.updateNovel(updated)
            existing.id
        } else {
            bookDao.insertNovel(novel.copy(
                inLibrary = true,
                coverUrl = metadata.coverUrl,
                author = metadata.author,
                summary = metadata.summary,
                totalChapters = if (metadata.totalChapters > 0) metadata.totalChapters else firstBatch.size
            )).toInt()
        }

        if (firstBatch.isNotEmpty()) {
            val chaptersWithId = firstBatch.mapIndexed { index, chapter ->
                chapter.copy(novelId = novelId, index = index)
            }
            bookDao.insertChapters(chaptersWithId)
        }
    }

    suspend fun loadMoreChapters(novelId: Int): Int {
        val novel = bookDao.getNovelById(novelId) ?: return 0
        val currentChapters = bookDao.getChapterList(novelId)


        val pageSize = 50
        val pageToFetch = (currentChapters.size / 100) + 1

        if (currentChapters.size >= novel.totalChapters && novel.totalChapters > 0) return 0

        val newChapters = scraper.getChaptersBatch(novel.url, pageToFetch)

        if (newChapters.isNotEmpty()) {
            val startIndex = currentChapters.size
            val chaptersWithId = newChapters.mapIndexed { index, chapter ->
                chapter.copy(novelId = novelId, index = startIndex + index)
            }

            val uniqueChapters = chaptersWithId.filter { newCh ->
                currentChapters.none { it.url == newCh.url }
            }

            if (uniqueChapters.isNotEmpty()) {
                bookDao.insertChapters(uniqueChapters)
                return uniqueChapters.size
            }
        }
        return 0
    }

    suspend fun downloadChapterContent(chapterId: Int): String {
        val chapter = bookDao.getChapterById(chapterId) ?: return ""
        if (chapter.content.isNotEmpty()) return chapter.content

        val content = scraper.getChapterContent(chapter.url)
        if (content.length > 50) {
            bookDao.updateChapter(chapter.copy(content = content))
        }
        return content
    }

    suspend fun downloadChapterExplicitly(chapterId: Int) {
        val chapter = bookDao.getChapterById(chapterId) ?: return
        val content = if (chapter.content.isNotEmpty()) chapter.content else scraper.getChapterContent(chapter.url)

        if (content.length > 50) {
            bookDao.updateChapter(chapter.copy(content = content, isDownloaded = true))
        }
    }

    fun downloadChapters(novelId: Int, limit: Int, startIndex: Int): Flow<Float> = flow {
        val allChapters = bookDao.getChapterList(novelId)
        val candidateChapters = allChapters.filter { it.index >= startIndex }
        val targetChapters = if (limit == -1) candidateChapters else candidateChapters.take(limit)

        var downloadedCount = 0
        val totalToDownload = targetChapters.count { !it.isDownloaded }.toFloat().takeIf { it > 0 } ?: 1f

        for (chapter in targetChapters) {
            if (!chapter.isDownloaded) {
                try {
                    val content = scraper.getChapterContent(chapter.url)
                    if (content.length > 50) {
                        bookDao.updateChapter(chapter.copy(content = content, isDownloaded = true))
                    }
                } catch (e: Exception) {
                    Log.e("Repo", "Download failed for ${chapter.title}: ${e.message}")
                }
                kotlinx.coroutines.delay(800)
                downloadedCount++
                emit(downloadedCount / totalToDownload)
            }
        }
        emit(1.0f)
    }

    private fun buildClient(url: String) {
        try {
            val formattedUrl = if (url.endsWith("/")) url else "$url/"
            val client = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(formattedUrl)
                .client(client)
                .build()

            api = retrofit.create(AllTalkApi::class.java)
            currentBaseUrl = formattedUrl
        } catch (e: Exception) {
            Log.e("TTS_REPO", "Init error: ${e.message}")
            api = null
        }
    }

    fun setServerUrl(newUrl: String) { buildClient(newUrl) }

    suspend fun testConnection(): Boolean {
        return withContext(Dispatchers.IO) {
            val currentApi = api ?: return@withContext false
            try {
                currentApi.generateAudio("test", "check_connection", "en", "standard", "false", "female_01.wav")
                true
            } catch (e: Exception) { false }
        }
    }

    suspend fun fetchAudioFromServer(text: String): Result<File> {
        return withContext(Dispatchers.IO) {
            val currentApi = api ?: return@withContext Result.failure(Exception("Invalid Server URL"))
            try {
                currentApi.generateAudio(text, "male_04.wav", "en", "standard", "false", "female_01.wav")
                val response = currentApi.downloadAudio("android_output.wav")
                val file = saveToTempFile(response)
                Result.success(file)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun saveToTempFile(body: ResponseBody): File {
        val file = File.createTempFile("tts_final", ".wav", cacheDir)
        val inputStream = body.byteStream()
        val outputStream = FileOutputStream(file)
        inputStream.use { input -> outputStream.use { output -> input.copyTo(output) } }
        return file
    }
}