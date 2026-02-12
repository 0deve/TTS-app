package com.example.tts_app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.work.WorkManager
import com.example.tts_app.data.PreferencesManager
import com.example.tts_app.data.TtsRepository
import com.example.tts_app.data.local.BookDao
import com.example.tts_app.player.AudioPlayerManager
import com.example.tts_app.player.LocalTtsManager

class MainViewModelFactory(
    private val context: Context,
    private val repository: TtsRepository,
    private val audioPlayer: AudioPlayerManager,
    private val localTts: LocalTtsManager,
    private val bookDao: BookDao,
    private val workManager: WorkManager,
    private val preferencesManager: PreferencesManager
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(context, repository, audioPlayer, localTts, bookDao, workManager, preferencesManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}