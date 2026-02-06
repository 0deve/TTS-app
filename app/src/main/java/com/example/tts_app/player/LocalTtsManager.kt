package com.example.tts_app.player

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

class LocalTtsManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    var onCompletionListener: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("LOCAL_TTS", "Language not supported")
            } else {
                isInitialized = true
                setupListener()
            }
        } else {
            Log.e("LOCAL_TTS", "Error")
        }
    }

    private fun setupListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                onCompletionListener?.invoke()
            }

            override fun onError(utteranceId: String?) {
                onCompletionListener?.invoke()
            }
        })
    }

    fun setSpeed(speed: Float) {
        if (isInitialized) {
            tts?.setSpeechRate(speed)
        }
    }

    fun speak(text: String) {
        if (isInitialized) {
            val params = android.os.Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "id_${System.currentTimeMillis()}")
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "id_${System.currentTimeMillis()}")
        } else {
            Log.e("LOCAL_TTS", "Not ready yet")
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
    }

    fun getAvailableVoices(): List<String> {
        return try {
            tts?.voices?.map { it.name }?.sorted() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun setVoice(voiceName: String) {
        try {
            val voice = tts?.voices?.find { it.name == voiceName }
            if (voice != null) {
                tts?.voice = voice
            }
        } catch (e: Exception) {
            Log.e("LOCAL_TTS", "Failed to set voice: $voiceName")
        }
    }
}