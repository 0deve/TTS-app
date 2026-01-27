package com.example.tts_app.player

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import java.io.File

class AudioPlayerManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    var onCompletionListener: (() -> Unit)? = null

    fun playFile(file: File) {
        try {
            stop()

            Log.d("AUDIO_PLAYER", "Classic: ${file.absolutePath}")
            Log.d("AUDIO_PLAYER", "Size: ${file.length()} bytes")

            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.fromFile(file))

                setOnPreparedListener { mp ->
                    Log.d("AUDIO_PLAYER", "Duration: ${mp.duration} ms")
                    mp.start()
                    Log.d("AUDIO_PLAYER", "From the beggining")
                }

                setOnErrorListener { _, what, extra ->
                    Log.e("AUDIO_PLAYER", "error MediaPlayer: What=$what Extra=$extra")
                    onCompletionListener?.invoke()
                    true
                }

                setOnCompletionListener {
                    Log.d("AUDIO_PLAYER", "End")
                    onCompletionListener?.invoke()
                }

                prepareAsync()
            }

        } catch (e: Exception) {
            Log.e("AUDIO_PLAYER", "init error: ${e.message}", e)
            onCompletionListener?.invoke()
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
        onCompletionListener = null
    }
}