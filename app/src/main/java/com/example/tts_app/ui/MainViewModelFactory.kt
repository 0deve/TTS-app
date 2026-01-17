package com.example.tts_app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.tts_app.data.TtsRepository
import com.example.tts_app.player.AudioPlayerManager


class MainViewModelFactory(
    private val repository: TtsRepository,
    private val audioPlayer: AudioPlayerManager
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, audioPlayer) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}