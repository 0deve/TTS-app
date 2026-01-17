package com.example.tts_app.player

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import java.io.File

class AudioPlayerManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null

    fun playFile(file: File) {
        try {
            stop()

            Log.d("AUDIO_PLAYER", "Incercam redarea cu MediaPlayer clasic: ${file.absolutePath}")
            Log.d("AUDIO_PLAYER", "Marime fizica: ${file.length()} bytes")

            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.fromFile(file))

                setOnPreparedListener { mp ->
                    Log.d("AUDIO_PLAYER", "GATA! Durata detectata: ${mp.duration} ms")
                    mp.start()
                    Log.d("AUDIO_PLAYER", "Redarea a inceput!")
                }

                setOnErrorListener { _, what, extra ->
                    Log.e("AUDIO_PLAYER", "Eroare MediaPlayer: What=$what Extra=$extra")
                    true
                }

                setOnCompletionListener {
                    Log.d("AUDIO_PLAYER", "Redare finalizata.")
                }

                prepareAsync()
            }

        } catch (e: Exception) {
            Log.e("AUDIO_PLAYER", "Eroare critica la initializare: ${e.message}", e)
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {

        }
    }

    fun release() {
        stop()
    }
}