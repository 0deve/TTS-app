package com.example.tts_app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.tts_app.data.TtsRepository
import com.example.tts_app.data.local.BookDao
import com.example.tts_app.player.AudioPlayerManager
import com.example.tts_app.player.LocalTtsManager

class MainViewModelFactory(
    private val repository: TtsRepository,
    private val audioPlayer: AudioPlayerManager,
    private val localTts: LocalTtsManager,
    private val bookDao: BookDao
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, audioPlayer, localTts, bookDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}