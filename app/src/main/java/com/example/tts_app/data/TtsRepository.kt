package com.example.tts_app.data

import android.content.Context
import android.util.Log
import com.example.tts_app.api.AllTalkApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory // Daca folosesti Gson, altfel sterge linia asta daca nu ai converter
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class TtsRepository(context: Context) {

    private val serverUrl = "ip"
    private val defaultVoice = "male_04.wav"

    private var api: AllTalkApi? = null
    private val cacheDir = context.cacheDir

    init {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(90, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .writeTimeout(90, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(serverUrl)
                .client(client)
                .build()

            api = retrofit.create(AllTalkApi::class.java)
            Log.d("TTS_REPO", "API success: $serverUrl")

        } catch (e: Exception) {
            Log.e("TTS_REPO", "API error: ${e.message}")
            api = null
        }
    }

    suspend fun fetchAudioFromServer(text: String): Result<File> {
        return withContext(Dispatchers.IO) {
            val currentApi = api
            if (currentApi == null) {
                return@withContext Result.failure(Exception("Wrong server config"))
            }

            try {
                Log.d("TTS_REPO", "generating")

                currentApi.generateAudio(
                    text = text,
                    voice = defaultVoice,
                    narratorVoice = defaultVoice
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