package com.example.tts_app.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tts_app.data.TtsRepository
import com.example.tts_app.data.local.Book
import com.example.tts_app.data.local.BookDao
import com.example.tts_app.player.AudioPlayerManager
import com.example.tts_app.player.LocalTtsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    private val _chapterLines = MutableStateFlow<List<String>>(emptyList())
    val chapterLines: StateFlow<List<String>> = _chapterLines.asStateFlow()

    private val _currentPlaybackIndex = MutableStateFlow(-1)
    val currentPlaybackIndex: StateFlow<Int> = _currentPlaybackIndex.asStateFlow()

    private var playbackQueue: List<String> = emptyList()
    private var isPlaying = false

    init {
        audioPlayer.onCompletionListener = {
            playNextSegment()
        }
        localTts.onCompletionListener = {
            playNextSegment()
        }
    }

    fun setTtsMode(useServer: Boolean) {
        stopAudio()
        _isServerTtsEnabled.value = useServer
    }

    private fun parseTextToLines(text: String): List<String> {
        return text.split(Regex("(?<=[.!?])\\s+|\n")).filter { it.isNotBlank() }
    }

    fun loadChapterText(text: String) {
        val lines = parseTextToLines(text)
        _chapterLines.value = lines
        stopAudio()
    }

    fun generateAudio(text: String) {
        stopAudio()
        playbackQueue = listOf(text)
        _currentPlaybackIndex.value = -1
        playSegment(0)
    }

    fun playFromIndex(index: Int) {
        stopAudio()
        val lines = _chapterLines.value
        if (index in lines.indices) {
            playbackQueue = lines
            playSegment(index)
        }
    }

    private fun playSegment(index: Int) {
        if (index >= playbackQueue.size || index < 0) {
            _uiState.value = UiState.Idle
            _currentPlaybackIndex.value = -1
            isPlaying = false
            return
        }

        isPlaying = true
        _currentPlaybackIndex.value = index
        val textToPlay = playbackQueue[index]

        if (_isServerTtsEnabled.value) {
            _uiState.value = UiState.Loading
            viewModelScope.launch {
                val result = repository.fetchAudioFromServer(textToPlay)
                result.onSuccess { file ->
                    if (isPlaying && _currentPlaybackIndex.value == index) {
                        _uiState.value = UiState.Success
                        audioPlayer.playFile(file)
                    }
                }.onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Server Error")
                    isPlaying = false
                }
            }
        } else {
            _uiState.value = UiState.Success
            localTts.speak(textToPlay)
        }
    }

    private fun playNextSegment() {
        viewModelScope.launch(Dispatchers.Main) {
            if (isPlaying) {
                val nextIndex = _currentPlaybackIndex.value + 1
                playSegment(nextIndex)
            }
        }
    }

    fun stopAudio() {
        isPlaying = false
        audioPlayer.stop()
        localTts.stop()
        _uiState.value = UiState.Idle
        _currentPlaybackIndex.value = -1
    }

    fun importBookMock(title: String, fullText: String) {
        viewModelScope.launch {
            val chapters = fullText.split("\n\n").filter { it.isNotBlank() }
            val newBook = Book(title = title, content = fullText, totalChapters = chapters.size)
            bookDao.insertBook(newBook)
        }
    }

    fun openBook(bookId: Int) {
        viewModelScope.launch {
            val book = bookDao.getBookById(bookId)
            _activeBook.value = book
            if (book != null) {
                val chapters = book.content.split("\n\n").filter { it.isNotBlank() }
                val currentText = chapters.getOrElse(book.currentChapterIndex) { "" }
                loadChapterText(currentText)
            }
        }
    }

    fun updateProgress(book: Book, newIndex: Int) {
        viewModelScope.launch {
            val updated = book.copy(currentChapterIndex = newIndex, lastAccessed = System.currentTimeMillis())
            bookDao.updateBook(updated)
            _activeBook.value = updated

            val chapters = updated.content.split("\n\n").filter { it.isNotBlank() }
            val currentText = chapters.getOrElse(newIndex) { "" }
            loadChapterText(currentText)
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