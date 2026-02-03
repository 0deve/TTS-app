package com.example.tts_app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tts_app.data.TtsRepository
import com.example.tts_app.data.local.BookDao
import com.example.tts_app.data.local.Chapter
import com.example.tts_app.data.local.Novel
import com.example.tts_app.player.AudioPlayerManager
import com.example.tts_app.player.LocalTtsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: TtsRepository,
    private val audioPlayer: AudioPlayerManager,
    private val localTts: LocalTtsManager,
    private val bookDao: BookDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _isServerTtsEnabled = MutableStateFlow(true)
    val isServerTtsEnabled = _isServerTtsEnabled.asStateFlow()
    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode = _isDarkMode.asStateFlow()
    private val _ttsSpeed = MutableStateFlow(1.0f)
    val ttsSpeed = _ttsSpeed.asStateFlow()
    private val _fontSize = MutableStateFlow(18f)
    val fontSize = _fontSize.asStateFlow()
    private val _serverIp = MutableStateFlow("http://192.168.1.2:8774")
    val serverIp = _serverIp.asStateFlow()
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.None)
    val connectionState = _connectionState.asStateFlow()

    val libraryNovels = bookDao.getLibraryNovels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val downloadedNovels = repository.getDownloadedNovels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeNovel = MutableStateFlow<Novel?>(null)
    val activeNovel = _activeNovel.asStateFlow()

    private val _activeChapters = MutableStateFlow<List<Chapter>>(emptyList())
    val activeChapters = _activeChapters.asStateFlow()

    private val _chapterLines = MutableStateFlow<List<String>>(emptyList())
    val chapterLines = _chapterLines.asStateFlow()

    private val _currentPlaybackIndex = MutableStateFlow(-1)
    val currentPlaybackIndex = _currentPlaybackIndex.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress = _downloadProgress.asStateFlow()

    private val _isChapterSortAscending = MutableStateFlow(true)
    val isChapterSortAscending = _isChapterSortAscending.asStateFlow()

    private var downloadJob: Job? = null
    private var novelJob: Job? = null

    private var playbackQueue: List<String> = emptyList()

    init {
        audioPlayer.onCompletionListener = { playNextSegment() }
        localTts.onCompletionListener = { playNextSegment() }
        repository.setServerUrl(_serverIp.value)
    }

    fun openNovelDetails(novelId: Int, filterDownloaded: Boolean = false) {
        novelJob?.cancel()
        novelJob = viewModelScope.launch {
            val novel = bookDao.getNovelById(novelId)
            _activeNovel.value = novel
            if (novel != null) {
                bookDao.getChapters(novelId).collect { allChapters ->
                    if (filterDownloaded) {
                        _activeChapters.value = allChapters.filter { it.isDownloaded }
                    } else {
                        _activeChapters.value = allChapters
                    }
                }
            }
        }
    }

    fun toggleChapterSort() { _isChapterSortAscending.value = !_isChapterSortAscending.value }

    fun downloadSingleChapter(chapter: Chapter) {
        viewModelScope.launch(Dispatchers.IO) { repository.downloadChapterExplicitly(chapter.id) }
    }

    fun playFromIndex(chapterIndex: Int, autoPlay: Boolean = true) {
        val novel = _activeNovel.value ?: return
        if (novel.currentChapterIndex != chapterIndex) {
            updateProgress(novel, chapterIndex)
        }

        val chapter = _activeChapters.value.find { it.index == chapterIndex } ?: return
        loadChapterContent(chapter, autoPlay)
    }

    fun onSegmentClick(index: Int) {
        if (index in playbackQueue.indices) {
            stopAudio()
            playAudioSegment(index)
        }
    }

    private fun loadChapterContent(chapter: Chapter, autoPlay: Boolean) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            stopAudio()

            val content = repository.downloadChapterContent(chapter.id)
            if (content.isNotEmpty()) {
                val lines = content.split(Regex("(?<=[.!?])\\s+|\n")).filter { it.isNotBlank() }
                _chapterLines.value = lines
                playbackQueue = lines

                _currentPlaybackIndex.value = 0
                if (autoPlay) {
                    playAudioSegment(0)
                }

                _uiState.value = UiState.Idle
            } else {
                _uiState.value = UiState.Error("Failed to load content")
            }
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            stopAudio()
            _isPlaying.value = false
        } else {
            val index = if (_currentPlaybackIndex.value == -1) 0 else _currentPlaybackIndex.value
            playAudioSegment(index)
        }
    }

    private fun playAudioSegment(index: Int) {
        if (index >= playbackQueue.size || index < 0) {
            _uiState.value = UiState.Idle
            _currentPlaybackIndex.value = -1
            _isPlaying.value = false
            return
        }

        _isPlaying.value = true
        _currentPlaybackIndex.value = index
        val text = playbackQueue[index]

        if (_isServerTtsEnabled.value) {
            _uiState.value = UiState.Loading
            viewModelScope.launch {
                repository.fetchAudioFromServer(text).onSuccess { file ->
                    if(_isPlaying.value && _currentPlaybackIndex.value == index) {
                        _uiState.value = UiState.Success
                        audioPlayer.playFile(file, _ttsSpeed.value)
                    }
                }.onFailure {
                    _uiState.value = UiState.Error(it.message ?: "Error")
                    _isPlaying.value = false
                }
            }
        } else {
            localTts.speak(text)
        }
    }

    private fun playNextSegment() {
        viewModelScope.launch(Dispatchers.Main) {
            if(_isPlaying.value) playAudioSegment(_currentPlaybackIndex.value + 1)
        }
    }

    fun stopAudio() {
        _isPlaying.value = false
        audioPlayer.stop()
        localTts.stop()
    }

    fun updateProgress(novel: Novel, newIndex: Int) {
        viewModelScope.launch {
            val updated = novel.copy(currentChapterIndex = newIndex)
            bookDao.updateNovel(updated)
            if (_activeNovel.value?.id == novel.id) {
                _activeNovel.value = updated
            }
        }
    }

    fun downloadNovel(novelId: Int, amount: Int, mode: String = "all") {
        if (downloadJob?.isActive == true) return
        downloadJob = viewModelScope.launch(Dispatchers.IO) {
            _downloadProgress.value = 0f
            if (mode == "unread") {
                val novel = bookDao.getNovelById(novelId) ?: return@launch
                repository.downloadChapters(novelId, amount, novel.currentChapterIndex).collect { _downloadProgress.value = it }
            } else {
                repository.downloadChapters(novelId, amount, 0).collect { _downloadProgress.value = it }
            }
            _downloadProgress.value = null
        }
    }

    fun cancelDownload() { downloadJob?.cancel(); _downloadProgress.value = null }
    fun searchNovels(query: String, onResult: (List<Novel>) -> Unit) { viewModelScope.launch { onResult(repository.searchRemoteNovels(query)) } }
    fun addToLibrary(novel: Novel) { viewModelScope.launch { repository.addToLibrary(novel) } }
    fun setTtsMode(enabled: Boolean) { _isServerTtsEnabled.value = enabled; stopAudio() }
    fun setFontSize(size: Float) { _fontSize.value = size }
    fun setTtsSpeed(speed: Float) { _ttsSpeed.value = speed; if(!_isServerTtsEnabled.value) localTts.setSpeed(speed) }
    fun updateServerIp(ip: String) { _serverIp.value = ip; repository.setServerUrl(ip) }
    fun testServerConnection() { viewModelScope.launch { _connectionState.value = ConnectionState.Testing; val s = repository.testConnection(); _connectionState.value = if(s) ConnectionState.Success else ConnectionState.Failed } }
    fun generateAudio(text: String) { stopAudio(); playbackQueue = listOf(text); playAudioSegment(0) }

    override fun onCleared() { super.onCleared(); audioPlayer.release(); localTts.shutdown() }
}