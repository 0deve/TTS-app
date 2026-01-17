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

    private val serverUrl = "ip"
    private val defaultVoice = "male_04.wav"

    private val api: AllTalkApi
    private val cacheDir = context.cacheDir

    init {
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
    }

    suspend fun fetchAudioFromServer(text: String): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("TTS_REPO", "PASUL 1: Comanda generare...")

                api.generateAudio(
                    text = text,
                    voice = defaultVoice,
                    narratorVoice = defaultVoice
                )

                Log.d("TTS_REPO", "Generare terminata pe server. PASUL 2: Descarcare...")


                val response = api.downloadAudio("android_output.wav")

                val file = saveToTempFile(response)

                Log.d("TTS_REPO", "Fisier final descarcat: ${file.length()} bytes")

                Result.success(file)
            } catch (e: Exception) {
                Log.e("TTS_REPO", "Eroare: ${e.message}", e)
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