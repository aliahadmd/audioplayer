package me.aliahad.audioplayer

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

data class AudioTrack(
    val title: String,
    val uri: Uri
)

data class PlayerUiState(
    val folderUri: Uri? = null,
    val tracks: List<AudioTrack> = emptyList(),
    val currentTrackIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class AudioPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val applicationContext = application.applicationContext

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _uiState.update { state ->
                state.copy(currentTrackIndex = currentIndex())
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { state -> state.copy(isPlaying = isPlaying) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                _uiState.update { state -> state.copy(isPlaying = false) }
            }
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
            } else {
                setPlaylist(folderUri, tracks)
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
        player.pause()
        if (player.mediaItemCount > 0 && currentIndex() >= 0) {
            player.seekTo(currentIndex(), 0L)
        }
        _uiState.update { state -> state.copy(isPlaying = false) }
    }

    fun selectTrack(index: Int, playImmediately: Boolean = true) {
        if (index !in _uiState.value.tracks.indices) return

        player.seekTo(index, 0L)
        _uiState.update { state -> state.copy(currentTrackIndex = index) }
        if (playImmediately) {
            startPlayback()
        }
    }

    override fun onCleared() {
        super.onCleared()
        player.removeListener(playerListener)
        player.release()
    }

    private suspend fun loadTracks(folderUri: Uri): List<AudioTrack> = withContext(Dispatchers.IO) {
        val documentFile = DocumentFile.fromTreeUri(applicationContext, folderUri)
        if (documentFile == null || !documentFile.isDirectory) {
            return@withContext emptyList()
        }

        val collection = mutableListOf<AudioTrack>()
        collectAudioFiles(documentFile, collection)
        collection.sortBy { it.title.lowercase(Locale.ROOT) }
        collection
    }

    private fun collectAudioFiles(folder: DocumentFile, collection: MutableList<AudioTrack>) {
        folder.listFiles().forEach { file ->
            when {
                file.isDirectory -> collectAudioFiles(file, collection)
                file.isFile && isAudioFile(file) -> {
                    val title = file.name ?: "Unknown"
                    collection += AudioTrack(title = title, uri = file.uri)
                }
            }
        }
    }

    private fun isAudioFile(file: DocumentFile): Boolean {
        val mimeType = file.type
        if (mimeType != null && mimeType.startsWith("audio")) return true

        val extension = file.name
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase(Locale.ROOT)
        return extension in SUPPORTED_AUDIO_EXTENSIONS
    }

    private fun setPlaylist(folderUri: Uri, tracks: List<AudioTrack>) {
        player.stop()
        player.clearMediaItems()

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

        player.setMediaItems(mediaItems, /* startIndex = */ 0, /* startPositionMs = */ 0L)
        player.prepare()

        _uiState.update { state ->
            state.copy(
                folderUri = folderUri,
                tracks = tracks,
                currentTrackIndex = if (tracks.isNotEmpty()) 0 else -1,
                isPlaying = false,
                isLoading = false,
                errorMessage = null
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
    }

    private fun currentIndex(): Int =
        if (player.mediaItemCount == 0) -1 else player.currentMediaItemIndex

    companion object {
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
