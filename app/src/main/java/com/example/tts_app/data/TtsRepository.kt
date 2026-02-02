package com.example.tts_app.data

import android.content.Context
import android.util.Log
import com.example.tts_app.api.AllTalkApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class TtsRepository(context: Context) {

    private var api: AllTalkApi? = null
    private val cacheDir = context.cacheDir
    private var currentBaseUrl: String = "http://127.0.0.1:8774"

    init {
        buildClient(currentBaseUrl)
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
            Log.d("TTS_REPO", "API rebuilt: $formattedUrl")

        } catch (e: Exception) {
            Log.e("TTS_REPO", "Init error: ${e.message}")
            api = null
        }
    }

    fun setServerUrl(newUrl: String) {
        buildClient(newUrl)
    }

    suspend fun testConnection(): Boolean {
        return withContext(Dispatchers.IO) {
            val currentApi = api ?: return@withContext false
            try {
                currentApi.generateAudio(
                    text = "test",
                    voice = "check_connection",
                    narratorVoice = "check_connection"
                )
                true
            } catch (e: retrofit2.HttpException) {
                true
            } catch (e: Exception) {
                Log.e("TTS_REPO", "Connection failed: ${e.message}")
                false
            }
        }
    }

    suspend fun fetchAudioFromServer(text: String): Result<File> {
        return withContext(Dispatchers.IO) {
            val currentApi = api
            if (currentApi == null) {
                return@withContext Result.failure(Exception("Invalid Server URL"))
            }

            try {
                Log.d("TTS_REPO", "generating")

                currentApi.generateAudio(
                    text = text,
                    voice = "male_04.wav",
                    narratorVoice = "male_04.wav"
                )

                Log.d("TTS_REPO", "done generating")

                val response = currentApi.downloadAudio("android_output.wav")
                val file = saveToTempFile(response)

                Log.d("TTS_REPO", "final file: ${file.length()} bytes")

                Result.success(file)
            } catch (e: Exception) {
                Log.e("TTS_REPO", "connection error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    private fun saveToTempFile(body: ResponseBody): File {
        val file = File.createTempFile("tts_final", ".wav", cacheDir)
        val inputStream = body.byteStream()
        val outputStream = FileOutputStream(file)

        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        return file
    }
}