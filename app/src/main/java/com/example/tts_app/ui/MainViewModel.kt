package com.example.tts_app.ui

import android.content.Context
import android.content.Intent
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
import com.example.tts_app.service.TtsService
import com.example.tts_app.workers.DownloadWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.net.Uri
import kotlinx.coroutines.withContext

data class AppStatistics(
    val totalNovels: Int = 0,
    val totalChaptersRead: Int = 0,
    val totalDownloadedChapters: Int = 0,
    val totalUnreadChapters: Int = 0
)

class MainViewModel(
    private val context: Context,
    private val repository: TtsRepository,
    private val audioPlayer: AudioPlayerManager,
    private val localTts: LocalTtsManager,
    private val bookDao: BookDao,
    private val workManager: WorkManager,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _isServerTtsEnabled = MutableStateFlow(preferencesManager.getBoolean(PreferencesManager.KEY_SERVER_ENABLED, false))
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

    private val _statistics = MutableStateFlow(AppStatistics())
    val statistics = _statistics.asStateFlow()

    private var novelJob: Job? = null
    private var playbackQueue: List<String> = emptyList()
    private var isTestMode = false

    private val playbackMutex = Mutex()

    init {
        audioPlayer.onCompletionListener = { playNextSegment() }
        localTts.onCompletionListener = { playNextSegment() }

        localTts.onInitSuccess = {
            if (!_isServerTtsEnabled.value) {
                updateAvailableVoices()
                localTts.setSpeed(_ttsSpeed.value)
                localTts.setPitch(_voicePitch.value)
            }
        }

        repository.setServerUrl(_serverIp.value)
        applyFont(_fontFamilyName.value)

        updateAvailableVoices()
        loadStatistics()
    }

    private fun updateService() {
        val novel = _activeNovel.value ?: return
        val chapter = _activeChapters.value.find { it.index == _viewingChapterIndex.value }
        val chapterTitle = chapter?.title ?: "Chapter ${_viewingChapterIndex.value}"

        val intent = Intent(context, TtsService::class.java).apply {
            putExtra("title", novel.title)
            putExtra("chapter", chapterTitle)
            putExtra("isPlaying", _isPlaying.value)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
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

        if (chapterIndex > novel.currentChapterIndex) {
            updateProgress(novel, chapterIndex)
        }

        val chapter = _activeChapters.value.find { it.index == chapterIndex } ?: return
        loadChapterContent(chapter, autoPlay)

        if (chapterIndex >= _activeChapters.value.size - 5) {
            loadMoreChapters()
        }
    }

    fun updateProgressIfThresholdMet(visibleItemIndex: Int, totalItems: Int) {
        val novel = _activeNovel.value ?: return
        val viewingIndex = _viewingChapterIndex.value

        if (viewingIndex == -1 || totalItems == 0) return

        preferencesManager.saveInt("novel_${novel.id}_chapter_${viewingIndex}_segment", visibleItemIndex)

        if (viewingIndex < novel.currentChapterIndex) {
            if (visibleItemIndex > totalItems / 2) {
                updateProgress(novel, viewingIndex)
            }
        } else if (viewingIndex > novel.currentChapterIndex) {
            updateProgress(novel, viewingIndex)
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

                val savedSegment = preferencesManager.getInt("novel_${chapter.novelId}_chapter_${chapter.index}_segment", 0)
                val safeSegment = if (savedSegment in lines.indices) savedSegment else 0

                _currentPlaybackIndex.value = safeSegment
                _uiState.value = UiState.Idle
                if (autoPlay) {
                    playAudioSegment(safeSegment)
                }

                updateService()
            } else {
                _uiState.value = UiState.Error("Failed to load content")
            }
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            stopAudio()
            _isPlaying.value = false
            updateService()
        } else {
            val index = if (_currentPlaybackIndex.value == -1) 0 else _currentPlaybackIndex.value
            playAudioSegment(index)
            updateService()
        }
    }

    fun playNext() {
        val novel = _activeNovel.value
        val chapters = _activeChapters.value
        val currentIndex = if (_viewingChapterIndex.value != -1) _viewingChapterIndex.value else novel?.currentChapterIndex ?: 0

        if (novel != null && chapters.isNotEmpty()) {
            val nextChapterIndex = currentIndex + 1
            if (nextChapterIndex < chapters.size) {
                updateProgress(novel, nextChapterIndex)
                playFromIndex(nextChapterIndex, autoPlay = true)
            }
        }
    }

    fun playPrevious() {
        val novel = _activeNovel.value
        val currentIndex = if (_viewingChapterIndex.value != -1) _viewingChapterIndex.value else novel?.currentChapterIndex ?: 0

        if (novel != null) {
            val prevChapterIndex = currentIndex - 1
            if (prevChapterIndex >= 0) {
                updateProgress(novel, prevChapterIndex)
                playFromIndex(prevChapterIndex, autoPlay = true)
            }
        }
    }

    fun retryAudio() {
        val index = if (_currentPlaybackIndex.value == -1) 0 else _currentPlaybackIndex.value
        playAudioSegment(index)
    }

    private fun playAudioSegment(index: Int) {
        viewModelScope.launch {
            playbackMutex.withLock {
                if (index >= playbackQueue.size || index < 0) {
                    _uiState.value = UiState.Idle
                    _currentPlaybackIndex.value = -1
                    _isPlaying.value = false
                    return@withLock
                }

                _isPlaying.value = true
                _currentPlaybackIndex.value = index

                val novel = _activeNovel.value
                if (novel != null) {
                    preferencesManager.saveInt("novel_${novel.id}_chapter_${_viewingChapterIndex.value}_segment", index)
                }

                val text = playbackQueue[index]

                if (_isServerTtsEnabled.value) {
                    _uiState.value = UiState.Loading
                    repository.fetchAudioFromServer(text, _selectedVoice.value).onSuccess { file ->
                        if (_isPlaying.value && _currentPlaybackIndex.value == index) {
                            _uiState.value = UiState.Success
                            audioPlayer.playFile(file, _ttsSpeed.value)
                        } else {
                            _uiState.value = UiState.Idle
                        }
                    }.onFailure {
                        _uiState.value = UiState.Error(it.message ?: "Error")
                        _isPlaying.value = false
                    }
                } else {
                    _uiState.value = UiState.Idle
                    localTts.setVoice(_selectedVoice.value)
                    localTts.speak(text)
                }
            }
        }
    }

    private fun playNextSegment() {
        viewModelScope.launch(Dispatchers.Main) {
            playbackMutex.withLock {
                if (!_isPlaying.value) return@withLock

                val nextIndex = _currentPlaybackIndex.value + 1
                if (nextIndex < playbackQueue.size) {
                    _isPlaying.value = true
                    _currentPlaybackIndex.value = nextIndex
                    val text = playbackQueue[nextIndex]

                    if (_isServerTtsEnabled.value) {
                        repository.fetchAudioFromServer(text, _selectedVoice.value).onSuccess { file ->
                            if (_isPlaying.value && _currentPlaybackIndex.value == nextIndex) {
                                audioPlayer.playFile(file, _ttsSpeed.value)
                            }
                        }
                    } else {
                        localTts.speak(text)
                    }
                } else {
                    if (isTestMode) {
                        _isPlaying.value = false
                        audioPlayer.stop()
                        localTts.stop()
                        return@withLock
                    }
                    val novel = _activeNovel.value
                    val chapters = _activeChapters.value
                    if (novel != null && chapters.isNotEmpty()) {
                        val currentIdx = if (_viewingChapterIndex.value != -1) _viewingChapterIndex.value else novel.currentChapterIndex
                        val nextChapterIndex = currentIdx + 1
                        val nextChapter = chapters.find { it.index == nextChapterIndex }
                        if (nextChapter != null) {
                            val updated = novel.copy(currentChapterIndex = nextChapterIndex)
                            bookDao.updateNovel(updated)
                            if (_activeNovel.value?.id == novel.id) {
                                _activeNovel.value = updated
                            }

                            val chapter = chapters.find { it.index == nextChapterIndex }
                            if (chapter != null) {
                                _viewingChapterIndex.value = nextChapterIndex
                                _uiState.value = UiState.Loading
                                audioPlayer.stop()
                                localTts.stop()
                                val content = repository.downloadChapterContent(chapter.id)
                                if (content.isNotEmpty()) {
                                    val lines = content.split(Regex("(?<=[.!?])\\s+|\n")).filter { it.isNotBlank() }
                                    _chapterLines.value = lines
                                    playbackQueue = lines

                                    val savedSegment = preferencesManager.getInt("novel_${chapter.novelId}_chapter_${chapter.index}_segment", 0)
                                    val safeSegment = if (savedSegment in lines.indices) savedSegment else 0

                                    _currentPlaybackIndex.value = safeSegment
                                    _uiState.value = UiState.Idle

                                    playAudioSegment(safeSegment)
                                    updateService()
                                } else {
                                    _uiState.value = UiState.Error("Failed to load content")
                                    _isPlaying.value = false
                                    updateService()
                                }
                            }
                        } else {
                            viewModelScope.launch(Dispatchers.IO) {
                                var foundNext = false
                                if (chapters.size < novel.totalChapters) {
                                    try {
                                        val added = repository.loadMoreChapters(novel.id)
                                        if (added > 0) {
                                            val newChapters = bookDao.getChapterList(novel.id)
                                            _activeChapters.value = newChapters
                                            val newlyFetchedChapter = newChapters.find { it.index == nextChapterIndex }
                                            if (newlyFetchedChapter != null) {
                                                foundNext = true
                                                withContext(Dispatchers.Main) {
                                                    val updated = novel.copy(currentChapterIndex = nextChapterIndex)
                                                    bookDao.updateNovel(updated)
                                                    if (_activeNovel.value?.id == novel.id) {
                                                        _activeNovel.value = updated
                                                    }
                                                    _viewingChapterIndex.value = nextChapterIndex
                                                    _uiState.value = UiState.Loading
                                                    audioPlayer.stop()
                                                    localTts.stop()
                                                    val content = repository.downloadChapterContent(newlyFetchedChapter.id)
                                                    if (content.isNotEmpty()) {
                                                        val lines = content.split(Regex("(?<=[.!?])\\s+|\n")).filter { it.isNotBlank() }
                                                        _chapterLines.value = lines
                                                        playbackQueue = lines

                                                        val savedSegment = preferencesManager.getInt("novel_${newlyFetchedChapter.novelId}_chapter_${newlyFetchedChapter.index}_segment", 0)
                                                        val safeSegment = if (savedSegment in lines.indices) savedSegment else 0

                                                        _currentPlaybackIndex.value = safeSegment
                                                        _uiState.value = UiState.Idle
                                                        playAudioSegment(safeSegment)
                                                        updateService()
                                                    } else {
                                                        _uiState.value = UiState.Error("Failed to load content")
                                                        _isPlaying.value = false
                                                        updateService()
                                                    }
                                                }
                                            }
                                        }
                                    } catch(e: Exception) {}
                                }
                                if (!foundNext) {
                                    withContext(Dispatchers.Main) {
                                        _isPlaying.value = false
                                        audioPlayer.stop()
                                        localTts.stop()
                                        updateService()
                                    }
                                }
                            }
                        }
                    } else {
                        _isPlaying.value = false
                        audioPlayer.stop()
                        localTts.stop()
                    }
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
            loadStatistics()
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
                            loadStatistics()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    fun cancelDownload() {
    }

    fun searchNovels(query: String, onResult: (List<Novel>) -> Unit) {
        viewModelScope.launch {
            val fetchedNovels = repository.searchRemoteNovels(query)
            val libraryNovels = bookDao.getAllNovelsSync()
            val libraryUrls = libraryNovels.map { it.url }.toSet()

            val mergedNovels = fetchedNovels.map { novel ->
                if (novel.url in libraryUrls) {
                    novel.copy(inLibrary = true)
                } else {
                    novel
                }
            }
            onResult(mergedNovels)
        }
    }

    fun addToLibrary(novel: Novel) {
        viewModelScope.launch {
            repository.addToLibrary(novel)
            loadStatistics()
        }
    }

    fun removeFromLibrary(novelId: Int) {
        viewModelScope.launch {
            bookDao.deleteNovel(novelId)
            loadStatistics()
        }
    }

    fun uninstallChapters(novelId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val chapters = bookDao.getChapterList(novelId)
            val updated = chapters.map { it.copy(content = "", isDownloaded = false) }
            bookDao.insertOrUpdateChapters(updated)
            loadStatistics()
        }
    }

    fun reloadAllNovelChaptersContent(novelId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val chapters = bookDao.getChapterList(novelId)
            val updated = chapters.map { it.copy(content = "", isDownloaded = false) }
            bookDao.insertOrUpdateChapters(updated)
        }
    }

    fun reloadCurrentChapter() {
        val chapterIndex = _viewingChapterIndex.value
        val chapter = _activeChapters.value.find { it.index == chapterIndex } ?: return
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            stopAudio()
            val content = repository.forceReloadChapterContent(chapter.id)
            if (content.isNotEmpty()) {
                val lines = content.split(Regex("(?<=[.!?])\\s+|\n")).filter { it.isNotBlank() }
                _chapterLines.value = lines
                playbackQueue = lines

                val savedSegment = preferencesManager.getInt("novel_${chapter.novelId}_chapter_${chapter.index}_segment", 0)
                val safeSegment = if (savedSegment in lines.indices) savedSegment else 0

                _currentPlaybackIndex.value = safeSegment
                _uiState.value = UiState.Idle
                updateService()
            } else {
                _uiState.value = UiState.Error("Failed to reload content")
            }
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
            loadStatistics()
        }
    }

    fun loadStatistics() {
        viewModelScope.launch(Dispatchers.IO) {
            val novels = bookDao.getAllNovelsSync().filter { it.inLibrary }
            val chapters = bookDao.getAllChaptersSync()

            val totalNovels = novels.size
            val readChapters = novels.sumOf { it.currentChapterIndex }
            val downloadedChapters = chapters.count { it.isDownloaded }
            val unreadChapters = novels.sumOf { (it.totalChapters - it.currentChapterIndex).coerceAtLeast(0) }

            _statistics.value = AppStatistics(
                totalNovels = totalNovels,
                totalChaptersRead = readChapters,
                totalDownloadedChapters = downloadedChapters,
                totalUnreadChapters = unreadChapters
            )
        }
    }

    override fun onCleared() { super.onCleared(); audioPlayer.release(); localTts.shutdown() }
}