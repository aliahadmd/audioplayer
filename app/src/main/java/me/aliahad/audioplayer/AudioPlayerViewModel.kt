package me.aliahad.audioplayer

import android.app.Application
import android.net.Uri
import android.media.MediaMetadataRetriever
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.max

data class AudioTrack(
    val title: String,
    val uri: Uri,
    val artist: String? = null,
    val album: String? = null,
    val durationMs: Long? = null,
    val fileSizeBytes: Long? = null
)

data class PlayerUiState(
    val folderUri: Uri? = null,
    val tracks: List<AudioTrack> = emptyList(),
    val currentTrackIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentPosition: Long = 0L,
    val bufferedPosition: Long = 0L,
    val duration: Long = 0L,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val playbackSpeed: Float = 1f
)

class AudioPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val applicationContext = application.applicationContext
    private val preferences = PlayerPreferences(applicationContext)
    private var progressJob: Job? = null

    init {
        viewModelScope.launch {
            val savedPreferences = runCatching { preferences.getPreferences() }.getOrNull()
                ?: return@launch
            val folderUriString = savedPreferences.folderUri ?: return@launch
            val folderUri = runCatching { Uri.parse(folderUriString) }.getOrNull()
                ?: return@launch
            restorePlaylist(folderUri, savedPreferences)
        }
    }

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val newIndex = currentIndex()
            _uiState.update { state ->
                state.copy(currentTrackIndex = newIndex)
            }
            updateProgressState()
            persistSelection(newIndex, resetPosition = true)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { state -> state.copy(isPlaying = isPlaying) }
            if (isPlaying) {
                startProgressUpdates()
            } else {
                stopProgressUpdates()
                persistCurrentState()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                _uiState.update { state -> state.copy(isPlaying = false) }
                stopProgressUpdates()
            }
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            _uiState.update { state -> state.copy(playbackSpeed = playbackParameters.speed) }
            persistCurrentState()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _uiState.update { state -> state.copy(isShuffleEnabled = shuffleModeEnabled) }
            persistCurrentState()
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _uiState.update { state -> state.copy(repeatMode = repeatMode) }
            persistCurrentState()
        }
    }

    private val player: ExoPlayer = ExoPlayer.Builder(applicationContext)
        .build()
        .also { exoPlayer ->
            exoPlayer.addListener(playerListener)
        }

    fun onFolderSelected(folderUri: Uri) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isLoading = true, errorMessage = null)
            }

            val tracks = loadTracks(folderUri)

            if (tracks.isEmpty()) {
                clearPlaylist()
                _uiState.update { state ->
                    state.copy(
                        folderUri = folderUri,
                        tracks = emptyList(),
                        currentTrackIndex = -1,
                        isPlaying = false,
                        isLoading = false,
                        errorMessage = "No audio files found in selected folder."
                    )
                }
                preferences.clear()
                return@launch
            } else {
                val startIndex = 0
                setPlaylist(folderUri, tracks, startIndex = startIndex)
                persistSelection(startIndex, resetPosition = true)
            }
        }
    }

    fun togglePlayPause() {
        val currentTracks = _uiState.value.tracks
        if (currentTracks.isEmpty()) return

        if (_uiState.value.isPlaying) {
            player.pause()
        } else {
            if (currentIndex() == -1) {
                selectTrack(0, playImmediately = true)
            } else {
                startPlayback()
            }
        }
    }

    fun playNext() {
        if (player.hasNextMediaItem()) {
            player.seekToNext()
            startPlayback()
        }
    }

    fun playPrevious() {
        if (player.hasPreviousMediaItem()) {
            player.seekToPrevious()
            startPlayback()
        } else {
            // If we're at the first track, restart it to mimic basic player behavior.
            if (player.mediaItemCount > 0) {
                player.seekTo(0, 0L)
                startPlayback()
            }
        }
    }

    fun stopPlayback() {
        val index = currentIndex()
        player.pause()
        if (player.mediaItemCount > 0 && index >= 0) {
            player.seekTo(index, 0L)
        }
        _uiState.update { state -> state.copy(isPlaying = false) }
        stopProgressUpdates()
        persistSelection(index, resetPosition = true)
    }

    fun selectTrack(index: Int, playImmediately: Boolean = true) {
        if (index !in _uiState.value.tracks.indices) return

        player.seekTo(index, 0L)
        _uiState.update { state -> state.copy(currentTrackIndex = index) }
        updateProgressState()
        persistSelection(index, resetPosition = true)
        if (playImmediately) {
            startPlayback()
        }
    }

    fun toggleShuffle() {
        val enabled = !player.shuffleModeEnabled
        player.shuffleModeEnabled = enabled
        _uiState.update { state -> state.copy(isShuffleEnabled = enabled) }
        persistCurrentState()
    }

    fun cycleRepeatMode() {
        val nextMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        player.repeatMode = nextMode
        _uiState.update { state -> state.copy(repeatMode = nextMode) }
        persistCurrentState()
    }

    fun cyclePlaybackSpeed() {
        val currentSpeed = player.playbackParameters.speed
        val nextSpeed = SPEED_PRESETS.firstOrNull { it > currentSpeed + SPEED_EPSILON }
            ?: SPEED_PRESETS.first()
        player.setPlaybackSpeed(nextSpeed)
        _uiState.update { state -> state.copy(playbackSpeed = nextSpeed) }
        persistCurrentState()
    }

    fun seekTo(positionMs: Long) {
        val duration = player.duration.takeIf { it != C.TIME_UNSET && it >= 0 } ?: Long.MAX_VALUE
        val safePosition = positionMs.coerceIn(0L, duration)
        player.seekTo(safePosition)
        updateProgressState()
        persistCurrentState()
    }

    private suspend fun restorePlaylist(folderUri: Uri, preferencesData: PlayerPreferencesData) {
        _uiState.update { state ->
            state.copy(isLoading = true, errorMessage = null)
        }

        val tracks = loadTracks(folderUri)

        if (tracks.isEmpty()) {
            clearPlaylist()
            preferences.clear()
            _uiState.update { state ->
                state.copy(
                    folderUri = folderUri,
                    tracks = emptyList(),
                    currentTrackIndex = -1,
                    isPlaying = false,
                    isLoading = false,
                    errorMessage = "We couldn't find audio in the saved folder. Please choose another folder."
                )
            }
            return
        }

        val targetIndex = preferencesData.currentTrackUri
            ?.let { runCatching { Uri.parse(it) }.getOrNull() }
            ?.let { storedUri -> tracks.indexOfFirst { it.uri == storedUri } }
            ?.takeIf { it >= 0 }
            ?: 0

        setPlaylist(folderUri, tracks, startIndex = targetIndex)
        player.shuffleModeEnabled = preferencesData.shuffleEnabled
        player.repeatMode = preferencesData.repeatMode
        if (preferencesData.playbackSpeed > 0f) {
            player.setPlaybackSpeed(preferencesData.playbackSpeed)
        }
        val savedPosition = preferencesData.positionMs.takeIf { it > 0L }
        if (savedPosition != null) {
            player.seekTo(targetIndex, savedPosition)
        }
        updateProgressState()
        _uiState.update { state ->
            state.copy(
                isShuffleEnabled = player.shuffleModeEnabled,
                repeatMode = player.repeatMode,
                playbackSpeed = player.playbackParameters.speed
            )
        }
        persistCurrentState()
    }

    override fun onCleared() {
        super.onCleared()
        player.removeListener(playerListener)
        stopProgressUpdates()
        persistCurrentState()
        player.release()
    }

    private suspend fun loadTracks(folderUri: Uri): List<AudioTrack> = withContext(Dispatchers.IO) {
        try {
            val documentFile = DocumentFile.fromTreeUri(applicationContext, folderUri)
            if (documentFile == null || !documentFile.isDirectory) {
                return@withContext emptyList()
            }

            val collection = mutableListOf<AudioTrack>()
            collectAudioFiles(documentFile, collection)
            collection.sortBy { it.title.lowercase(Locale.ROOT) }
            collection
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    private fun collectAudioFiles(folder: DocumentFile, collection: MutableList<AudioTrack>) {
        val children = try {
            folder.listFiles()
        } catch (_: SecurityException) {
            return
        }
        children.forEach { file ->
            when {
                file.isDirectory -> collectAudioFiles(file, collection)
                file.isFile && isAudioFile(file) -> {
                    collection += buildAudioTrack(file)
                }
            }
        }
    }

    private fun buildAudioTrack(file: DocumentFile): AudioTrack {
        val defaultTitle = file.name ?: "Unknown"
        val metadataRetriever = MediaMetadataRetriever()
        var artist: String? = null
        var album: String? = null
        var durationMs: Long? = null
        var title: String? = null

        runCatching {
            metadataRetriever.setDataSource(applicationContext, file.uri)
            title = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            artist = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            album = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            durationMs = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
        }.onFailure {
            // Ignore metadata failures; fall back to file information.
        }.also {
            runCatching { metadataRetriever.release() }
        }

        val cleanTitle = title?.takeIf { it.isNotBlank() } ?: defaultTitle
        val cleanArtist = artist?.takeIf { it.isNotBlank() }
        val cleanAlbum = album?.takeIf { it.isNotBlank() }
        val fileSize = file.length().takeIf { it >= 0 }

        return AudioTrack(
            title = cleanTitle,
            uri = file.uri,
            artist = cleanArtist,
            album = cleanAlbum,
            durationMs = durationMs,
            fileSizeBytes = fileSize
        )
    }

    private fun isAudioFile(file: DocumentFile): Boolean {
        val mimeType = file.type
        if (mimeType != null && mimeType.startsWith("audio")) return true

        val extension = file.name
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase(Locale.ROOT)
        return extension in SUPPORTED_AUDIO_EXTENSIONS
    }

    private fun setPlaylist(folderUri: Uri, tracks: List<AudioTrack>, startIndex: Int = 0) {
        player.stop()
        player.clearMediaItems()

        if (tracks.isEmpty()) {
            _uiState.update { state ->
                state.copy(
                    folderUri = folderUri,
                    tracks = emptyList(),
                    currentTrackIndex = -1,
                    isPlaying = false,
                    isLoading = false,
                    errorMessage = null,
                    currentPosition = 0L,
                    bufferedPosition = 0L,
                    duration = 0L
                )
            }
            return
        }

        val mediaItems = tracks.map { track ->
            MediaItem.Builder()
                .setUri(track.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .build()
                )
                .build()
        }

        val effectiveIndex = startIndex.coerceIn(0, tracks.lastIndex)
        player.setMediaItems(mediaItems, effectiveIndex, 0L)
        player.prepare()

        _uiState.update { state ->
            state.copy(
                folderUri = folderUri,
                tracks = tracks,
                currentTrackIndex = effectiveIndex,
                isPlaying = false,
                isLoading = false,
                errorMessage = null,
                currentPosition = 0L,
                bufferedPosition = 0L,
                duration = player.duration.takeIf { it != C.TIME_UNSET && it >= 0 } ?: 0L,
                isShuffleEnabled = player.shuffleModeEnabled,
                repeatMode = player.repeatMode,
                playbackSpeed = player.playbackParameters.speed
            )
        }
    }

    private fun clearPlaylist() {
        player.stop()
        player.clearMediaItems()
    }

    private fun startPlayback() {
        if (player.playbackState == Player.STATE_IDLE) {
            player.prepare()
        }
        player.play()
        _uiState.update { state -> state.copy(isPlaying = true, currentTrackIndex = currentIndex()) }
        startProgressUpdates()
    }

    private fun currentIndex(): Int =
        if (player.mediaItemCount == 0) -1 else player.currentMediaItemIndex

    private fun persistSelection(index: Int, resetPosition: Boolean = false) {
        val folderUri = _uiState.value.folderUri ?: return
        val track = _uiState.value.tracks.getOrNull(index)
        val positionOverride = if (resetPosition) 0L else null
        persistCurrentState(positionOverride = positionOverride, trackOverride = track)
    }

    private fun persistCurrentState(
        positionOverride: Long? = null,
        trackOverride: AudioTrack? = null
    ) {
        val folderUri = _uiState.value.folderUri
        val track = trackOverride ?: _uiState.value.tracks.getOrNull(currentIndex())
        val position = positionOverride ?: player.currentPosition
        val shuffleEnabled = player.shuffleModeEnabled
        val repeatMode = player.repeatMode
        val playbackSpeed = player.playbackParameters.speed
        viewModelScope.launch {
            preferences.saveState(
                folderUri = folderUri,
                trackUri = track?.uri,
                positionMs = position,
                shuffleEnabled = shuffleEnabled,
                repeatMode = repeatMode,
                playbackSpeed = playbackSpeed
            )
        }
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                updateProgressState()
                delay(PROGRESS_UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
        updateProgressState()
    }

    private fun updateProgressState() {
        val duration = player.duration.takeIf { it != C.TIME_UNSET && it >= 0 } ?: 0L
        val position = player.currentPosition.coerceAtLeast(0L)
        val buffered = player.bufferedPosition.coerceAtLeast(0L)
        _uiState.update { state ->
            state.copy(
                currentPosition = position,
                bufferedPosition = buffered,
                duration = max(duration, position)
            )
        }
    }

    companion object {
        private const val PROGRESS_UPDATE_INTERVAL_MS = 500L
        private val SPEED_PRESETS = listOf(0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
        private const val SPEED_EPSILON = 0.05f
        private val SUPPORTED_AUDIO_EXTENSIONS = setOf(
            "mp3",
            "wav",
            "m4a",
            "aac",
            "ogg",
            "flac"
        )
    }
}
