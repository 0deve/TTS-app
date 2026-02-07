package com.example.tts_app.ui

import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.tts_app.data.PreferencesManager
import com.example.tts_app.data.TtsRepository
import com.example.tts_app.data.local.BookDao
import com.example.tts_app.data.local.Chapter
import com.example.tts_app.data.local.Novel
import com.example.tts_app.player.AudioPlayerManager
import com.example.tts_app.player.LocalTtsManager
import com.example.tts_app.workers.DownloadWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: TtsRepository,
    private val audioPlayer: AudioPlayerManager,
    private val localTts: LocalTtsManager,
    private val bookDao: BookDao,
    private val workManager: WorkManager,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _isServerTtsEnabled = MutableStateFlow(preferencesManager.getBoolean(PreferencesManager.KEY_SERVER_ENABLED, true))
    val isServerTtsEnabled = _isServerTtsEnabled.asStateFlow()

    private val _ttsSpeed = MutableStateFlow(preferencesManager.getFloat(PreferencesManager.KEY_TTS_SPEED, 1.0f))
    val ttsSpeed = _ttsSpeed.asStateFlow()

    private val _voicePitch = MutableStateFlow(preferencesManager.getFloat(PreferencesManager.KEY_VOICE_PITCH, 1.0f))
    val voicePitch = _voicePitch.asStateFlow()

    private val _fontSize = MutableStateFlow(preferencesManager.getFloat(PreferencesManager.KEY_FONT_SIZE, 18f))
    val fontSize = _fontSize.asStateFlow()

    private val _fontColor = MutableStateFlow(preferencesManager.getLong(PreferencesManager.KEY_FONT_COLOR, 0xFFF9FAFB))
    val fontColor = _fontColor.asStateFlow()

    private val _serverIp = MutableStateFlow(preferencesManager.getString(PreferencesManager.KEY_SERVER_IP, "http://192.168.1.2:8774"))
    val serverIp = _serverIp.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.None)
    val connectionState = _connectionState.asStateFlow()

    private val _lineHeightMultiplier = MutableStateFlow(preferencesManager.getFloat(PreferencesManager.KEY_LINE_HEIGHT, 1.5f))
    val lineHeightMultiplier = _lineHeightMultiplier.asStateFlow()

    private val _textMargin = MutableStateFlow(preferencesManager.getInt(PreferencesManager.KEY_TEXT_MARGIN, 24))
    val textMargin = _textMargin.asStateFlow()

    private val _fontFamily = MutableStateFlow(FontFamily.Default)
    val fontFamily = _fontFamily.asStateFlow()
    private val _fontFamilyName = MutableStateFlow(preferencesManager.getString(PreferencesManager.KEY_FONT_FAMILY, "Default"))
    val fontFamilyName = _fontFamilyName.asStateFlow()

    private val _isOledMode = MutableStateFlow(preferencesManager.getBoolean(PreferencesManager.KEY_OLED_MODE, false))
    val isOledMode = _isOledMode.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<String>>(emptyList())
    val availableVoices = _availableVoices.asStateFlow()
    private val _selectedVoice = MutableStateFlow(preferencesManager.getString(PreferencesManager.KEY_SELECTED_VOICE, "male_04.wav"))
    val selectedVoice = _selectedVoice.asStateFlow()

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

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore = _isLoadingMore.asStateFlow()

    private val _viewingChapterIndex = MutableStateFlow(-1)
    val viewingChapterIndex = _viewingChapterIndex.asStateFlow()

    private var novelJob: Job? = null
    private var playbackQueue: List<String> = emptyList()
    private var isTestMode = false

    init {
        audioPlayer.onCompletionListener = { playNextSegment() }
        localTts.onCompletionListener = { playNextSegment() }

        localTts.onInitSuccess = {
            if (!_isServerTtsEnabled.value) {
                updateAvailableVoices()
            }
        }

        repository.setServerUrl(_serverIp.value)
        applyFont(_fontFamilyName.value)

        if (!_isServerTtsEnabled.value) {
            localTts.setSpeed(_ttsSpeed.value)
            localTts.setPitch(_voicePitch.value)
        }
        updateAvailableVoices()
    }

    private fun updateAvailableVoices() {
        viewModelScope.launch {
            if (_isServerTtsEnabled.value) {
                _availableVoices.value = repository.getServerVoices()
            } else {
                _availableVoices.value = localTts.getAvailableVoices()
            }
        }
    }

    fun checkLibraryUpdates() {
        if (_isLoadingMore.value) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                val novels = bookDao.getLibraryNovels().first()
                novels.forEach { novel ->
                    try {
                        repository.loadMoreChapters(novel.id)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun openNovelDetails(novelId: Int, filterDownloaded: Boolean = false) {
        novelJob?.cancel()
        novelJob = viewModelScope.launch {
            val novel = bookDao.getNovelById(novelId)

            if (novel != null && novel.hasUnseenUpdates) {
                val updatedNovel = novel.copy(hasUnseenUpdates = false)
                bookDao.updateNovel(updatedNovel)
                _activeNovel.value = updatedNovel
            } else {
                _activeNovel.value = novel
            }

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

    fun loadMoreChapters() {
        val novel = _activeNovel.value ?: return
        if (_isLoadingMore.value) return

        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                repository.loadMoreChapters(novel.id)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to load more chapters")
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun loadAllChapters() {
        val novel = _activeNovel.value ?: return
        if (_isLoadingMore.value) return

        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                var addedCount = 1
                while (addedCount > 0) {
                    val currentCount = bookDao.getChapterList(novel.id).size
                    if (novel.totalChapters > 0 && currentCount >= novel.totalChapters) break
                    addedCount = repository.loadMoreChapters(novel.id)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to load all chapters")
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun toggleChapterSort() { _isChapterSortAscending.value = !_isChapterSortAscending.value }

    fun downloadSingleChapter(chapter: Chapter) {
        viewModelScope.launch(Dispatchers.IO) { repository.downloadChapterExplicitly(chapter.id) }
    }

    fun playFromIndex(chapterIndex: Int, autoPlay: Boolean = true) {
        isTestMode = false
        val novel = _activeNovel.value ?: return

        _viewingChapterIndex.value = chapterIndex

        val chapter = _activeChapters.value.find { it.index == chapterIndex } ?: return
        loadChapterContent(chapter, autoPlay)

        if (chapterIndex >= _activeChapters.value.size - 5) {
            loadMoreChapters()
        }
    }

    fun updateProgressIfThresholdMet(visibleItemIndex: Int, totalItems: Int) {
        val novel = _activeNovel.value ?: return
        val viewingIndex = _viewingChapterIndex.value

        if (totalItems > 0 && visibleItemIndex > totalItems / 2) {
            if (viewingIndex != -1 && viewingIndex != novel.currentChapterIndex) {
                updateProgress(novel, viewingIndex)
            }
        }
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

    fun retryAudio() {
        val index = if (_currentPlaybackIndex.value == -1) 0 else _currentPlaybackIndex.value
        playAudioSegment(index)
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
                repository.fetchAudioFromServer(text, _selectedVoice.value).onSuccess { file ->
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
            localTts.setVoice(_selectedVoice.value)
            localTts.speak(text)
        }
    }

    private fun playNextSegment() {
        viewModelScope.launch(Dispatchers.Main) {
            if (!_isPlaying.value) return@launch

            val nextIndex = _currentPlaybackIndex.value + 1
            if (nextIndex < playbackQueue.size) {
                playAudioSegment(nextIndex)
            } else {
                if (isTestMode) {
                    stopAudio()
                    return@launch
                }
                val novel = _activeNovel.value
                val chapters = _activeChapters.value
                if (novel != null && chapters.isNotEmpty()) {
                    val nextChapterIndex = novel.currentChapterIndex + 1
                    val nextChapter = chapters.find { it.index == nextChapterIndex }
                    if (nextChapter != null) {
                        playFromIndex(nextChapterIndex, autoPlay = true)
                    } else {
                        if (chapters.size < novel.totalChapters) {
                            loadMoreChapters()
                        }
                        stopAudio()
                    }
                } else {
                    stopAudio()
                }
            }
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
        val workData = workDataOf(
            "novelId" to novelId,
            "limit" to amount,
            "mode" to mode
        )

        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workData)
            .addTag("download_$novelId")
            .build()

        workManager.enqueueUniqueWork("download_$novelId", ExistingWorkPolicy.KEEP, workRequest)

        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(workRequest.id).collect { workInfo ->
                if (workInfo != null) {
                    when (workInfo.state) {
                        WorkInfo.State.RUNNING -> {
                            val progress = workInfo.progress.getFloat("progress", 0f)
                            _downloadProgress.value = progress
                        }
                        WorkInfo.State.SUCCEEDED, WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> {
                            _downloadProgress.value = null
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    fun cancelDownload() {
    }

    fun searchNovels(query: String, onResult: (List<Novel>) -> Unit) { viewModelScope.launch { onResult(repository.searchRemoteNovels(query)) } }
    fun addToLibrary(novel: Novel) { viewModelScope.launch { repository.addToLibrary(novel) } }
    fun removeFromLibrary(novelId: Int) { viewModelScope.launch { bookDao.deleteNovel(novelId) } }
    fun uninstallChapters(novelId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val chapters = bookDao.getChapterList(novelId)
            val updated = chapters.map { it.copy(content = "", isDownloaded = false) }
            bookDao.insertChapters(updated)
        }
    }

    fun setTtsMode(enabled: Boolean) {
        _isServerTtsEnabled.value = enabled
        preferencesManager.saveBoolean(PreferencesManager.KEY_SERVER_ENABLED, enabled)
        stopAudio()
        updateAvailableVoices()
    }
    fun setFontSize(size: Float) {
        _fontSize.value = size
        preferencesManager.saveFloat(PreferencesManager.KEY_FONT_SIZE, size)
    }
    fun setTtsSpeed(speed: Float) {
        _ttsSpeed.value = speed
        preferencesManager.saveFloat(PreferencesManager.KEY_TTS_SPEED, speed)
        if(!_isServerTtsEnabled.value) localTts.setSpeed(speed)
    }
    fun setVoicePitch(pitch: Float) {
        _voicePitch.value = pitch
        preferencesManager.saveFloat(PreferencesManager.KEY_VOICE_PITCH, pitch)
        if(!_isServerTtsEnabled.value) localTts.setPitch(pitch)
    }
    fun setFontColor(color: Long) {
        _fontColor.value = color
        preferencesManager.saveLong(PreferencesManager.KEY_FONT_COLOR, color)
    }
    fun updateServerIp(ip: String) {
        _serverIp.value = ip
        preferencesManager.saveString(PreferencesManager.KEY_SERVER_IP, ip)
        repository.setServerUrl(ip); updateAvailableVoices()
    }
    fun testServerConnection() { viewModelScope.launch { _connectionState.value = ConnectionState.Testing; val s = repository.testConnection(); _connectionState.value = if(s) ConnectionState.Success else ConnectionState.Failed; updateAvailableVoices() } }

    fun generateAudio(text: String) {
        isTestMode = true
        stopAudio()
        playbackQueue = listOf(text)
        playAudioSegment(0)
    }

    fun setLineHeight(multiplier: Float) {
        _lineHeightMultiplier.value = multiplier
        preferencesManager.saveFloat(PreferencesManager.KEY_LINE_HEIGHT, multiplier)
    }
    fun setTextMargin(margin: Int) {
        _textMargin.value = margin
        preferencesManager.saveInt(PreferencesManager.KEY_TEXT_MARGIN, margin)
    }
    fun setFontFamily(name: String) {
        _fontFamilyName.value = name
        preferencesManager.saveString(PreferencesManager.KEY_FONT_FAMILY, name)
        applyFont(name)
    }

    private fun applyFont(name: String) {
        _fontFamily.value = when(name) {
            "Serif" -> FontFamily.Serif
            "SansSerif" -> FontFamily.SansSerif
            "Monospace" -> FontFamily.Monospace
            else -> FontFamily.Default
        }
    }

    fun setSelectedVoice(voice: String) {
        _selectedVoice.value = voice
        preferencesManager.saveString(PreferencesManager.KEY_SELECTED_VOICE, voice)
    }

    fun setOledMode(enabled: Boolean) {
        _isOledMode.value = enabled
        preferencesManager.saveBoolean(PreferencesManager.KEY_OLED_MODE, enabled)
    }

    fun backupLibrary(uri: Uri) {
        viewModelScope.launch {
            repository.backupLibrary(uri)
        }
    }

    fun restoreLibrary(uri: Uri) {
        viewModelScope.launch {
            repository.restoreLibrary(uri)
        }
    }

    override fun onCleared() { super.onCleared(); audioPlayer.release(); localTts.shutdown() }
}