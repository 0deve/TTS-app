package com.example.tts_app.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tts_app.data.TtsRepository
import com.example.tts_app.data.local.Book
import com.example.tts_app.data.local.BookDao
import com.example.tts_app.player.AudioPlayerManager
import com.example.tts_app.player.LocalTtsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: TtsRepository,
    private val audioPlayer: AudioPlayerManager,
    private val localTts: LocalTtsManager,
    private val bookDao: BookDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _isServerTtsEnabled = MutableStateFlow(true)
    val isServerTtsEnabled: StateFlow<Boolean> = _isServerTtsEnabled.asStateFlow()

    val books = bookDao.getAllBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeBook = MutableStateFlow<Book?>(null)
    val activeBook: StateFlow<Book?> = _activeBook.asStateFlow()

    fun setTtsMode(useServer: Boolean) {
        stopAudio()
        _isServerTtsEnabled.value = useServer
    }

    fun generateAudio(text: String) {
        stopAudio()

        if (_isServerTtsEnabled.value) {

            _uiState.value = UiState.Loading
            viewModelScope.launch {
                val result = repository.fetchAudioFromServer(text)
                result.onSuccess { file ->
                    _uiState.value = UiState.Success
                    audioPlayer.playFile(file)
                }.onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Server Error")
                }
            }
        } else {
            _uiState.value = UiState.Success
            localTts.speak(text)
        }
    }

    fun stopAudio() {
        audioPlayer.stop()
        localTts.stop()
        _uiState.value = UiState.Idle
    }

    fun importBookMock(title: String, fullText: String) {
        viewModelScope.launch {
            val chapters = fullText.split("\n\n").filter { it.isNotBlank() }
            val newBook = Book(title = title, content = fullText, totalChapters = chapters.size)
            bookDao.insertBook(newBook)
        }
    }

    fun openBook(bookId: Int) {
        viewModelScope.launch { _activeBook.value = bookDao.getBookById(bookId) }
    }

    fun updateProgress(book: Book, newIndex: Int) {
        viewModelScope.launch {
            val updated = book.copy(currentChapterIndex = newIndex, lastAccessed = System.currentTimeMillis())
            bookDao.updateBook(updated)
            _activeBook.value = updated
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
        localTts.shutdown()
    }
}

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    object Success : UiState()
    data class Error(val message: String) : UiState()
}