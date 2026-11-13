package com.example.ui

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.graphics.Bitmap
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.AppDatabase
import com.example.data.PlayerSettings
import com.example.data.ThemeMode
import com.example.data.UserPreferences
import com.example.data.VideoEntity
import com.example.data.VideoMetadataExtractor
import com.example.data.VideoRepository
import com.example.player.EngineTelemetry
import com.example.player.VeloPlaybackService
import com.example.player.VeloPlayerHolder
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class FeedFilter(val label: String) {
    ALL("All"),
    CONTINUE_WATCHING("Continue watching"),
    HD_60FPS("60fps & HD"),
    FAVORITES("Favorites"),
    WATCH_LATER("Watch later")
}

enum class PlayerSheetMode {
    HIDDEN,
    MINI,
    EXPANDED,
    FULLSCREEN
}

enum class VideoResizeMode {
    FIT,
    ZOOM
}

data class PlayerUiState(
    val currentVideo: VideoEntity? = null,
    val mode: PlayerSheetMode = PlayerSheetMode.HIDDEN,
    val isPlaying: Boolean = false,
    val isEnded: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val pitchShift: Float = 1.0f,
    val pitchLockEnabled: Boolean = true,
    val isSpeedBoostActive: Boolean = false,
    val speedBoostLabel: String = "2.0x speed",
    val isLooping: Boolean = false,
    val captionsEnabled: Boolean = true,
    val isControlsLocked: Boolean = false,
    val isControlsVisible: Boolean = true,
    val resizeMode: VideoResizeMode = VideoResizeMode.FIT,
    val isScrubbing: Boolean = false,
    val scrubPreviewPositionMs: Long = 0L,
    val scrubPreviewBitmap: Bitmap? = null,
    val telemetry: EngineTelemetry = EngineTelemetry()
)

class VeloPlayViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val metadataExtractor = VideoMetadataExtractor(application)
    private val repository = VideoRepository(application, database.videoDao(), metadataExtractor)
    private val userPreferences = UserPreferences(application)
    val playerManager = VeloPlayerHolder.get(application)

    private var controllerFuture: ListenableFuture<MediaController>? = null

    private val _hasStoragePermission = MutableStateFlow(false)
    val hasStoragePermission: StateFlow<Boolean> = _hasStoragePermission.asStateFlow()

    private val _selectedFilter = MutableStateFlow(FeedFilter.ALL)
    val selectedFilter: StateFlow<FeedFilter> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchExpanded = MutableStateFlow(false)
    val isSearchExpanded: StateFlow<Boolean> = _isSearchExpanded.asStateFlow()

    private val _playerUiState = MutableStateFlow(PlayerUiState())
    val playerUiState: StateFlow<PlayerUiState> = _playerUiState.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val settings: StateFlow<PlayerSettings> = userPreferences.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlayerSettings()
    )

    val allVideos: StateFlow<List<VideoEntity>> = repository.videosFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val filteredVideos: StateFlow<List<VideoEntity>> = combine(
        allVideos,
        selectedFilter,
        searchQuery
    ) { videos, filter, query ->
        val byFilter = when (filter) {
            FeedFilter.ALL -> videos
            FeedFilter.CONTINUE_WATCHING -> videos.filter { it.lastPositionMs > 1000L }
            FeedFilter.HD_60FPS -> videos.filter { it.frameRate >= 50f || minOf(it.width, it.height) >= 720 }
            FeedFilter.FAVORITES -> videos.filter { it.isFavorite }
            FeedFilter.WATCH_LATER -> videos.filter { it.isWatchLater }
        }
        val q = query.trim().lowercase()
        if (q.isEmpty()) byFilter
        else byFilter.filter {
            it.title.lowercase().contains(q) || it.channelLabel.lowercase().contains(q)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val upNextVideos: StateFlow<List<VideoEntity>> = combine(
        allVideos,
        _playerUiState
    ) { videos, playerState ->
        val currentUri = playerState.currentVideo?.uri
        videos.filter { it.uri != currentUri }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private var preBoostSpeed: Float? = null
    private var openVideoJob: Job? = null
    private var speedSaveJob: Job? = null
    private var scrubJob: Job? = null
    private var lastSavedPositionMs: Long = 0L
    @Volatile private var isPlayerActive: Boolean = false

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playerUiState.update { it.copy(isPlaying = isPlaying) }
            if (!isPlaying) {
                persistCurrentProgress()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (!isPlayerActive || mediaItem == null) return
            val targetUri = mediaItem.mediaId
            val matched = allVideos.value.find { it.uri == targetUri } ?: return
            if (_playerUiState.value.currentVideo?.uri != matched.uri) {
                viewModelScope.launch {
                    val prefs = userPreferences.settingsFlow.first()
                    val speed = if (prefs.rememberSpeedPerVideo) matched.preferredSpeed else prefs.defaultSpeed
                    val pitch = if (prefs.rememberSpeedPerVideo) matched.preferredPitch else prefs.defaultPitch
                    val pitchLock = if (prefs.rememberSpeedPerVideo) matched.pitchLockEnabled else prefs.defaultPitchLock
                    playerManager.applySpeedAndPitch(speed, pitch, pitchLock, force = true)
                    _playerUiState.update {
                        it.copy(
                            currentVideo = matched,
                            playbackSpeed = speed,
                            pitchShift = pitch,
                            pitchLockEnabled = pitchLock,
                            durationMs = matched.durationMs,
                            isEnded = false
                        )
                    }
                }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (!isPlayerActive) return
            val current = _playerUiState.value.currentVideo ?: return
            val player = playerManager.player
            val duration = player.duration.takeIf { it > 0L } ?: current.durationMs
            _playerUiState.update {
                it.copy(
                    durationMs = duration,
                    isEnded = playbackState == Player.STATE_ENDED
                )
            }
            if (playbackState == Player.STATE_ENDED) {
                persistCurrentProgress(resetIfCompleted = true)
                if (isPlayerActive && settings.value.autoplayNext && !_playerUiState.value.isLooping) {
                    playNext()
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            repository.ensureBundledSamplesSeeded()
        }

        // Observe player telemetry
        viewModelScope.launch {
            playerManager.telemetry.collect { telemetry ->
                _playerUiState.update { it.copy(telemetry = telemetry) }
            }
        }

        // Sync currentVideo object if metadata updates in Room
        viewModelScope.launch {
            allVideos.collect { list ->
                val activeUri = _playerUiState.value.currentVideo?.uri ?: return@collect
                val updated = list.find { it.uri == activeUri }
                if (updated != null) {
                    _playerUiState.update { it.copy(currentVideo = updated) }
                }
            }
        }

        playerManager.player.addListener(playerListener)

        // Smooth position polling loop
        viewModelScope.launch {
            while (isActive) {
                val state = _playerUiState.value
                if (state.currentVideo != null && !state.isScrubbing) {
                    val player = playerManager.player
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    val dur = player.duration.takeIf { it > 0L } ?: state.currentVideo.durationMs
                    val buf = player.bufferedPosition.coerceAtLeast(0L)
                    _playerUiState.update {
                        it.copy(positionMs = pos, durationMs = dur, bufferedMs = buf)
                    }
                    if (player.isPlaying && kotlin.math.abs(pos - lastSavedPositionMs) >= 3000L) {
                        lastSavedPositionMs = pos
                        repository.saveProgress(state.currentVideo.uri, pos)
                    }
                }
                delay(100L)
            }
        }
    }

    fun syncDeviceVideos(hasPermission: Boolean) {
        _hasStoragePermission.value = hasPermission
        viewModelScope.launch {
            repository.syncDeviceVideos(hasPermission)
        }
    }

    fun selectFilter(filter: FeedFilter) {
        _selectedFilter.value = filter
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchExpanded(expanded: Boolean) {
        _isSearchExpanded.value = expanded
        if (!expanded) _searchQuery.value = ""
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun openVideo(video: VideoEntity, targetMode: PlayerSheetMode = PlayerSheetMode.EXPANDED) {
        openVideoJob?.cancel()
        isPlayerActive = true
        ensurePlaybackServiceConnected()

        openVideoJob = viewModelScope.launch {
            val prefs = userPreferences.settingsFlow.first()
            if (!isPlayerActive) return@launch
            val speed = if (prefs.rememberSpeedPerVideo) video.preferredSpeed else prefs.defaultSpeed
            val pitch = if (prefs.rememberSpeedPerVideo) video.preferredPitch else prefs.defaultPitch
            val pitchLock = if (prefs.rememberSpeedPerVideo) video.pitchLockEnabled else prefs.defaultPitchLock

            val startPos = if (video.durationMs > 5000L && video.lastPositionMs >= video.durationMs - 2000L) {
                0L
            } else {
                video.lastPositionMs
            }

            preBoostSpeed = null
            lastSavedPositionMs = startPos

            _playerUiState.update {
                it.copy(
                    currentVideo = video,
                    mode = targetMode,
                    positionMs = startPos,
                    durationMs = video.durationMs,
                    playbackSpeed = speed,
                    pitchShift = pitch,
                    pitchLockEnabled = pitchLock,
                    isSpeedBoostActive = false,
                    isControlsLocked = false,
                    isControlsVisible = true,
                    isEnded = false
                )
            }

            val queue = allVideos.value
            val startIndex = queue.indexOfFirst { it.uri == video.uri }
            if (startIndex >= 0) {
                playerManager.playPlaylist(
                    videos = queue,
                    startIndex = startIndex,
                    startPositionMs = startPos,
                    speed = speed,
                    pitch = pitch,
                    pitchLock = pitchLock,
                    loop = _playerUiState.value.isLooping
                )
            } else {
                playerManager.playVideo(
                    video = video,
                    startPositionMs = startPos,
                    speed = speed,
                    pitch = pitch,
                    pitchLock = pitchLock,
                    loop = _playerUiState.value.isLooping
                )
            }
            repository.saveProgress(video.uri, startPos)
        }
    }

    fun openExternalUri(uri: Uri) {
        viewModelScope.launch {
            val imported = repository.importSingleUri(uri)
            openVideo(imported, PlayerSheetMode.EXPANDED)
        }
    }

    private fun ensurePlaybackServiceConnected() {
        val app = getApplication<Application>()
        val future = controllerFuture
        if (future == null || future.isCancelled) {
            val token = SessionToken(app, ComponentName(app, VeloPlaybackService::class.java))
            controllerFuture = MediaController.Builder(app, token).buildAsync().also { created ->
                created.addListener({ runCatching { created.get() } }, ContextCompat.getMainExecutor(app))
            }
        }
    }

    fun setPlayerMode(mode: PlayerSheetMode) {
        _playerUiState.update { it.copy(mode = mode, isControlsVisible = true) }
    }

    fun minimizePlayer() {
        _playerUiState.update { it.copy(mode = PlayerSheetMode.MINI) }
    }

    fun expandPlayer() {
        _playerUiState.update { it.copy(mode = PlayerSheetMode.EXPANDED, isControlsVisible = true) }
    }

    fun toggleFullscreen() {
        val current = _playerUiState.value.mode
        val next = if (current == PlayerSheetMode.FULLSCREEN) {
            PlayerSheetMode.EXPANDED
        } else {
            PlayerSheetMode.FULLSCREEN
        }
        _playerUiState.update { it.copy(mode = next, isControlsVisible = true) }
    }

    fun closePlayer() {
        isPlayerActive = false
        openVideoJob?.cancel()
        speedSaveJob?.cancel()
        scrubJob?.cancel()
        persistCurrentProgress()
        playerManager.stopAndClear()
        _playerUiState.value = PlayerUiState(mode = PlayerSheetMode.HIDDEN)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        val app = getApplication<Application>()
        runCatching { app.stopService(Intent(app, VeloPlaybackService::class.java)) }
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun pausePlayer() {
        playerManager.player.pause()
    }

    fun seekBySeconds(deltaSeconds: Int) {
        playerManager.seekBy(deltaSeconds * 1000L)
        val pos = playerManager.player.currentPosition.coerceAtLeast(0L)
        _playerUiState.update { it.copy(positionMs = pos) }
    }

    fun playNext() {
        if (!isPlayerActive) return
        val currentMode = _playerUiState.value.mode
        if (currentMode == PlayerSheetMode.HIDDEN && _playerUiState.value.currentVideo == null) return
        val queue = upNextVideos.value
        if (queue.isNotEmpty()) {
            openVideo(queue.first(), if (currentMode == PlayerSheetMode.HIDDEN) PlayerSheetMode.EXPANDED else currentMode)
        }
    }

    fun playPrevious() {
        val pos = playerManager.player.currentPosition
        if (pos > 3000L) {
            playerManager.seekTo(0L)
        } else {
            val all = allVideos.value
            val current = _playerUiState.value.currentVideo ?: return
            val idx = all.indexOfFirst { it.uri == current.uri }
            if (idx > 0) {
                openVideo(all[idx - 1], _playerUiState.value.mode)
            } else {
                playerManager.seekTo(0L)
            }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 8.0f)
        val state = _playerUiState.value
        _playerUiState.update { it.copy(playbackSpeed = clamped, isSpeedBoostActive = false) }
        preBoostSpeed = null
        playerManager.applySpeedAndPitch(clamped, state.pitchShift, state.pitchLockEnabled)
        scheduleSpeedAndPitchPersist(state.currentVideo?.uri, clamped, state.pitchShift, state.pitchLockEnabled)
    }

    fun setPitchShift(pitch: Float) {
        val clamped = pitch.coerceIn(0.50f, 2.00f)
        val state = _playerUiState.value
        _playerUiState.update { it.copy(pitchShift = clamped) }
        playerManager.applySpeedAndPitch(state.playbackSpeed, clamped, state.pitchLockEnabled)
        scheduleSpeedAndPitchPersist(state.currentVideo?.uri, state.playbackSpeed, clamped, state.pitchLockEnabled)
    }

    fun setPitchLockEnabled(enabled: Boolean) {
        val state = _playerUiState.value
        _playerUiState.update { it.copy(pitchLockEnabled = enabled) }
        playerManager.applySpeedAndPitch(state.playbackSpeed, state.pitchShift, enabled)
        scheduleSpeedAndPitchPersist(state.currentVideo?.uri, state.playbackSpeed, state.pitchShift, enabled)
    }

    private fun scheduleSpeedAndPitchPersist(
        uri: String?,
        speed: Float,
        pitch: Float,
        pitchLock: Boolean
    ) {
        if (uri == null) return
        speedSaveJob?.cancel()
        speedSaveJob = viewModelScope.launch {
            delay(350L)
            repository.saveSpeedAndPitch(uri, speed, pitch, pitchLock)
        }
    }

    fun resetSpeedAndPitch() {
        val state = _playerUiState.value
        _playerUiState.update {
            it.copy(playbackSpeed = 1.0f, pitchShift = 1.0f, pitchLockEnabled = true, isSpeedBoostActive = false)
        }
        preBoostSpeed = null
        playerManager.applySpeedAndPitch(1.0f, 1.0f, true, force = true)
        state.currentVideo?.let { video ->
            viewModelScope.launch {
                repository.saveSpeedAndPitch(video.uri, 1.0f, 1.0f, true)
            }
        }
    }

    fun startHoldSpeedBoost() {
        val state = _playerUiState.value
        if (state.isSpeedBoostActive || state.isControlsLocked) return
        preBoostSpeed = state.playbackSpeed
        val boostSpeed = if (state.playbackSpeed < 1.9f) 2.0f else (state.playbackSpeed * 2f).coerceAtMost(8.0f)
        val label = if (boostSpeed == 2.0f) "2.0x speed" else "${String.format("%.1f", boostSpeed)}x turbo"
        _playerUiState.update {
            it.copy(
                playbackSpeed = boostSpeed,
                isSpeedBoostActive = true,
                speedBoostLabel = label
            )
        }
        playerManager.applySpeedAndPitch(boostSpeed, state.pitchShift, state.pitchLockEnabled)
    }

    fun endHoldSpeedBoost() {
        val restore = preBoostSpeed ?: return
        preBoostSpeed = null
        val state = _playerUiState.value
        _playerUiState.update {
            it.copy(playbackSpeed = restore, isSpeedBoostActive = false)
        }
        playerManager.applySpeedAndPitch(restore, state.pitchShift, state.pitchLockEnabled)
    }

    fun toggleLooping() {
        val next = !_playerUiState.value.isLooping
        _playerUiState.update { it.copy(isLooping = next) }
        playerManager.setLooping(next)
    }

    fun toggleCaptions() {
        val next = !_playerUiState.value.captionsEnabled
        _playerUiState.update { it.copy(captionsEnabled = next) }
        playerManager.setCaptionsEnabled(next)
    }

    fun toggleControlsLocked() {
        val next = !_playerUiState.value.isControlsLocked
        _playerUiState.update { it.copy(isControlsLocked = next, isControlsVisible = !next) }
    }

    fun setControlsVisible(visible: Boolean) {
        _playerUiState.update { it.copy(isControlsVisible = visible) }
    }

    fun toggleResizeMode() {
        val next = if (_playerUiState.value.resizeMode == VideoResizeMode.FIT) {
            VideoResizeMode.ZOOM
        } else {
            VideoResizeMode.FIT
        }
        _playerUiState.update { it.copy(resizeMode = next) }
    }

    fun onScrubStart(positionMs: Long) {
        playerManager.setScrubbingMode(true)
        val currentThumbPath = _playerUiState.value.currentVideo?.thumbnailPath
        _playerUiState.update {
            it.copy(isScrubbing = true, scrubPreviewPositionMs = positionMs)
        }
        if (_playerUiState.value.scrubPreviewBitmap == null && !currentThumbPath.isNullOrBlank()) {
            viewModelScope.launch {
                val fallback = metadataExtractor.loadCachedThumbnailBitmap(currentThumbPath)
                if (fallback != null && _playerUiState.value.isScrubbing && _playerUiState.value.scrubPreviewBitmap == null) {
                    _playerUiState.update { it.copy(scrubPreviewBitmap = fallback) }
                }
            }
        }
        requestScrubPreview(positionMs, immediate = true)
    }

    fun onScrubChange(positionMs: Long) {
        _playerUiState.update {
            it.copy(scrubPreviewPositionMs = positionMs, positionMs = positionMs)
        }
        requestScrubPreview(positionMs, immediate = false)
    }

    fun onScrubEnd(positionMs: Long) {
        scrubJob?.cancel()
        playerManager.seekTo(positionMs)
        playerManager.setScrubbingMode(false)
        _playerUiState.update {
            it.copy(
                isScrubbing = false,
                positionMs = positionMs,
                scrubPreviewBitmap = null
            )
        }
    }

    private fun requestScrubPreview(positionMs: Long, immediate: Boolean) {
        val uri = _playerUiState.value.currentVideo?.uri ?: return
        scrubJob?.cancel()
        scrubJob = viewModelScope.launch {
            if (!immediate) delay(28L)
            val frame = metadataExtractor.extractPreviewFrame(uri, positionMs)
            if (_playerUiState.value.isScrubbing && frame != null) {
                _playerUiState.update { it.copy(scrubPreviewBitmap = frame) }
            }
        }
    }

    fun toggleFavorite(video: VideoEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(video)
        }
    }

    fun toggleWatchLater(video: VideoEntity) {
        viewModelScope.launch {
            repository.toggleWatchLater(video)
        }
    }

    fun renameVideo(video: VideoEntity, newTitle: String) {
        viewModelScope.launch {
            repository.renameVideo(video.uri, newTitle)
        }
    }

    fun deleteVideo(video: VideoEntity) {
        viewModelScope.launch {
            if (_playerUiState.value.currentVideo?.uri == video.uri) {
                closePlayer()
            }
            repository.deleteVideo(video.uri)
            _statusMessage.value = "Removed \"${video.title}\""
        }
    }

    fun attachSubtitleToCurrentVideo(subtitleUri: Uri) {
        val current = _playerUiState.value.currentVideo ?: return
        val resumePos = playerManager.player.currentPosition.coerceAtLeast(0L)
        viewModelScope.launch {
            repository.attachSubtitle(current.uri, subtitleUri)
            val updated = current.copy(subtitleUri = subtitleUri.toString())
            _playerUiState.update { it.copy(currentVideo = updated, captionsEnabled = true) }
            playerManager.setCaptionsEnabled(true)
            playerManager.playVideo(
                video = updated,
                startPositionMs = resumePos,
                speed = _playerUiState.value.playbackSpeed,
                pitch = _playerUiState.value.pitchShift,
                pitchLock = _playerUiState.value.pitchLockEnabled,
                loop = _playerUiState.value.isLooping
            )
            _statusMessage.value = "Subtitles loaded"
        }
    }

    fun clearSubtitleFromCurrentVideo() {
        val current = _playerUiState.value.currentVideo ?: return
        val resumePos = playerManager.player.currentPosition.coerceAtLeast(0L)
        viewModelScope.launch {
            repository.clearSubtitle(current.uri)
            val updated = current.copy(subtitleUri = null)
            _playerUiState.update { it.copy(currentVideo = updated) }
            playerManager.playVideo(
                video = updated,
                startPositionMs = resumePos,
                speed = _playerUiState.value.playbackSpeed,
                pitch = _playerUiState.value.pitchShift,
                pitchLock = _playerUiState.value.pitchLockEnabled,
                loop = _playerUiState.value.isLooping
            )
            _statusMessage.value = "Subtitles cleared"
        }
    }

    fun setAutoplayNext(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setAutoplayNext(enabled) }
    }

    fun setRememberSpeedPerVideo(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setRememberSpeedPerVideo(enabled) }
    }

    fun setDefaultSpeed(speed: Float) {
        viewModelScope.launch { userPreferences.setDefaultSpeed(speed) }
    }

    fun setDoubleTapSeekSeconds(seconds: Int) {
        viewModelScope.launch { userPreferences.setDoubleTapSeekSeconds(seconds) }
    }

    fun setShowEngineStats(show: Boolean) {
        viewModelScope.launch { userPreferences.setShowEngineStats(show) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { userPreferences.setThemeMode(mode) }
    }

    fun cycleThemeMode() {
        val next = when (settings.value.themeMode) {
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
        }
        setThemeMode(next)
    }

    fun setBackgroundPlayEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setBackgroundPlayEnabled(enabled) }
    }

    fun setAutoPipEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setAutoPipEnabled(enabled) }
    }

    private fun persistCurrentProgress(resetIfCompleted: Boolean = false) {
        val current = _playerUiState.value.currentVideo ?: return
        val pos = if (resetIfCompleted) 0L else playerManager.player.currentPosition.coerceAtLeast(0L)
        lastSavedPositionMs = pos
        viewModelScope.launch {
            repository.saveProgress(current.uri, pos)
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.player.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
}
