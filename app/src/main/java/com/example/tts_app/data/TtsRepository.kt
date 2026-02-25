package com.example.tts_app.data

import android.content.Context
import android.net.Uri
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
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class TtsRepository(
    private val context: Context,
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
                status = if (metadata.status.isNotEmpty() && metadata.status != "Unknown") metadata.status else existing.status,
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
                status = metadata.status,
                totalChapters = if (metadata.totalChapters > 0) metadata.totalChapters else firstBatch.size
            )).toInt()
        }

        if (firstBatch.isNotEmpty()) {
            val chaptersWithId = firstBatch.mapIndexed { index, chapter ->
                chapter.copy(novelId = novelId, index = index)
            }
            bookDao.insertOrUpdateChapters(chaptersWithId)
        }
    }

    suspend fun loadMoreChapters(novelId: Int): Int {
        val novel = bookDao.getNovelById(novelId) ?: return 0
        val currentChapters = bookDao.getChapterList(novelId)

        val maxIndex = currentChapters.maxOfOrNull { it.index } ?: -1
        val pageToFetch = ((maxIndex + 1) / 100) + 1

        var newChapters = scraper.getChaptersBatch(novel.url, pageToFetch)
        if (newChapters.isEmpty() && pageToFetch > 1) {
            newChapters = scraper.getChaptersBatch(novel.url, pageToFetch - 1)
        }

        if (newChapters.isNotEmpty()) {
            val uniqueChapters = newChapters.filter { newCh ->
                currentChapters.none { it.url == newCh.url }
            }

            if (uniqueChapters.isNotEmpty()) {
                val startIndex = maxIndex + 1
                val chaptersWithId = uniqueChapters.mapIndexed { index, chapter ->
                    chapter.copy(novelId = novelId, index = startIndex + index)
                }

                bookDao.insertOrUpdateChapters(chaptersWithId)

                val newTotal = currentChapters.size + chaptersWithId.size
                if (newTotal > novel.totalChapters) {
                    bookDao.updateNovel(novel.copy(totalChapters = newTotal))
                }

                return chaptersWithId.size
            }
        }
        return 0
    }

    suspend fun downloadChapterContent(chapterId: Int): String {
        val chapter = bookDao.getChapterById(chapterId) ?: return ""
        if (chapter.content.isNotEmpty()) return chapter.content

        var content = ""
        for (i in 1..6) {
            content = scraper.getChapterContent(chapter.url)
            if (content.length > 50 && !content.startsWith("Error")) {
                bookDao.updateChapter(chapter.copy(content = content))
                return content
            }
            if (i < 6) kotlinx.coroutines.delay(5000)
        }
        return content
    }

    suspend fun forceReloadChapterContent(chapterId: Int): String {
        val chapter = bookDao.getChapterById(chapterId) ?: return ""
        var content = ""
        for (i in 1..6) {
            content = scraper.getChapterContent(chapter.url)
            if (content.length > 50 && !content.startsWith("Error")) {
                bookDao.updateChapter(chapter.copy(content = content, isDownloaded = true))
                return content
            }
            if (i < 6) kotlinx.coroutines.delay(5000)
        }
        return content
    }

    suspend fun downloadChapterExplicitly(chapterId: Int) {
        val chapter = bookDao.getChapterById(chapterId) ?: return
        var content = if (chapter.content.isNotEmpty()) chapter.content else ""
        if (content.isNotEmpty()) return

        for (i in 1..6) {
            content = scraper.getChapterContent(chapter.url)
            if (content.length > 50 && !content.startsWith("Error")) {
                bookDao.updateChapter(chapter.copy(content = content, isDownloaded = true))
                return
            }
            if (i < 6) kotlinx.coroutines.delay(5000)
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
                    var content = ""
                    for (i in 1..6) {
                        content = scraper.getChapterContent(chapter.url)
                        if (content.length > 50 && !content.startsWith("Error")) {
                            bookDao.updateChapter(chapter.copy(content = content, isDownloaded = true))
                            break
                        }
                        if (i < 6) kotlinx.coroutines.delay(5000)
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

    suspend fun fetchAudioFromServer(text: String, voice: String): Result<File> {
        return withContext(Dispatchers.IO) {
            val currentApi = api ?: return@withContext Result.failure(Exception("Invalid Server URL"))
            try {
                currentApi.generateAudio(text, voice, "en", "standard", "false", "female_01.wav")
                val response = currentApi.downloadAudio("android_output.wav")
                val file = saveToTempFile(response)
                Result.success(file)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getServerVoices(): List<String> {
        return withContext(Dispatchers.IO) {
            val currentApi = api ?: return@withContext emptyList()
            try {
                val response = currentApi.getVoices()
                val jsonString = response.string()
                val jsonArray = JSONArray(jsonString)
                val voices = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    voices.add(jsonArray.getString(i))
                }
                voices.sorted()
            } catch (e: Exception) {
                Log.e("TTS_REPO", "Failed to fetch voices: ${e.message}")
                listOf("male_04.wav", "female_01.wav")
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

    suspend fun backupLibrary(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val novels = bookDao.getAllNovelsSync()
            val chapters = bookDao.getAllChaptersSync()

            val root = JSONObject()
            val novelsArray = JSONArray()
            novels.forEach { novel ->
                val nObj = JSONObject()
                nObj.put("id", novel.id)
                nObj.put("url", novel.url)
                nObj.put("title", novel.title)
                nObj.put("coverUrl", novel.coverUrl)
                nObj.put("author", novel.author)
                nObj.put("summary", novel.summary)
                nObj.put("status", novel.status)
                nObj.put("inLibrary", novel.inLibrary)
                nObj.put("totalChapters", novel.totalChapters)
                nObj.put("currentChapterIndex", novel.currentChapterIndex)
                novelsArray.put(nObj)
            }

            val chaptersArray = JSONArray()
            chapters.forEach { chapter ->
                val cObj = JSONObject()
                cObj.put("novelId", chapter.novelId)
                cObj.put("index", chapter.index)
                cObj.put("title", chapter.title)
                cObj.put("url", chapter.url)
                cObj.put("content", chapter.content)
                cObj.put("isDownloaded", chapter.isDownloaded)
                cObj.put("releaseDate", chapter.releaseDate)
                chaptersArray.put(cObj)
            }

            root.put("novels", novelsArray)
            root.put("chapters", chaptersArray)

            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(root.toString().toByteArray())
            }
            true
        } catch (e: Exception) {
            Log.e("Backup", "Backup failed", e)
            false
        }
    }

    suspend fun restoreLibrary(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val stringBuilder = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stringBuilder.append(line)
                    }
                }
            }
            val json = JSONObject(stringBuilder.toString())
            val novelsArray = json.getJSONArray("novels")
            val chaptersArray = json.getJSONArray("chapters")

            val novels = mutableListOf<Novel>()
            for (i in 0 until novelsArray.length()) {
                val obj = novelsArray.getJSONObject(i)
                novels.add(Novel(
                    id = obj.getInt("id"),
                    url = obj.getString("url"),
                    title = obj.getString("title"),
                    coverUrl = obj.optString("coverUrl"),
                    author = obj.optString("author"),
                    summary = obj.optString("summary"),
                    status = obj.optString("status", "Unknown"),
                    inLibrary = obj.getBoolean("inLibrary"),
                    totalChapters = obj.getInt("totalChapters"),
                    currentChapterIndex = obj.getInt("currentChapterIndex")
                ))
            }

            val chapters = mutableListOf<Chapter>()
            for (i in 0 until chaptersArray.length()) {
                val obj = chaptersArray.getJSONObject(i)
                chapters.add(Chapter(
                    novelId = obj.getInt("novelId"),
                    index = obj.getInt("index"),
                    title = obj.getString("title"),
                    url = obj.getString("url"),
                    content = obj.optString("content"),
                    isDownloaded = obj.getBoolean("isDownloaded"),
                    releaseDate = obj.optString("releaseDate", "")
                ))
            }

            novels.forEach { bookDao.insertNovel(it) }
            bookDao.insertOrUpdateChapters(chapters)
            true
        } catch (e: Exception) {
            Log.e("Restore", "Restore failed", e)
            false
        }
    }
}