package com.example.tts_app.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tts_app.data.TtsRepository
import com.example.tts_app.player.AudioPlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(
    private val repository: TtsRepository,
    private val audioPlayer: AudioPlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun generateAudio(text: String) {
        _uiState.value = UiState.Loading

        viewModelScope.launch {
            Log.d("VIEWMODEL", "generare: $text")

            val result = repository.fetchAudioFromServer(text)

            result.onSuccess { file ->
                Log.d("VIEWMODEL", "succes")
                _uiState.value = UiState.Success

                audioPlayer.playFile(file)
            }

            result.onFailure { error ->
                Log.e("VIEWMODEL", "Eroare: ${error.message}")
                _uiState.value = UiState.Error(error.message ?: "Unknown error")
            }
        }
    }

    fun stopAudio() {
        audioPlayer.stop()
        _uiState.value = UiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    object Success : UiState()
    data class Error(val message: String) : UiState()
}