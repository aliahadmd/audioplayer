package me.aliahad.audioplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import me.aliahad.audioplayer.ui.theme.AudioplayerTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: AudioPlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AudioplayerTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val context = LocalContext.current
                val folderPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocumentTree()
                ) { uri ->
                    if (uri != null) {
                        val contentResolver = context.contentResolver
                        try {
                            contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        } catch (_: SecurityException) {
                            // Permission might already be granted; continue with selection.
                        }
                        viewModel.onFolderSelected(uri)
                    }
                }

                AudioPlayerScreen(
                    uiState = uiState,
                    onChooseFolder = { folderPicker.launch(null) },
                    onPlayPause = { viewModel.togglePlayPause() },
                    onNext = { viewModel.playNext() },
                    onPrevious = { viewModel.playPrevious() },
                    onStop = { viewModel.stopPlayback() },
                    onSelectTrack = { index -> viewModel.selectTrack(index) },
                    onSeekTo = { position -> viewModel.seekTo(position) },
                    onToggleShuffle = { viewModel.toggleShuffle() },
                    onCycleRepeatMode = { viewModel.cycleRepeatMode() },
                    onCyclePlaybackSpeed = { viewModel.cyclePlaybackSpeed() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(
    uiState: PlayerUiState,
    onChooseFolder: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onStop: () -> Unit,
    onSelectTrack: (Int) -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeatMode: () -> Unit,
    onCyclePlaybackSpeed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTrack = uiState.tracks.getOrNull(uiState.currentTrackIndex)
    val hasTracks = uiState.tracks.isNotEmpty()
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            MaterialTheme.colorScheme.surface
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Audio Player",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    actions = {
                        IconButton(onClick = onChooseFolder) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = "Choose folder"
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (uiState.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                var showDetails by rememberSaveable { mutableStateOf(false) }

                if (showDetails && currentTrack != null) {
                    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    TrackDetailsSheet(
                        track = currentTrack,
                        sheetState = sheetState,
                        onDismiss = { showDetails = false }
                    )
                }

                NowPlayingCard(
                    currentTrack = currentTrack,
                    isPlaying = uiState.isPlaying,
                    isLoading = uiState.isLoading,
                    folderUri = uiState.folderUri,
                    errorMessage = uiState.errorMessage,
                    onChooseFolder = onChooseFolder,
                    onShowDetails = { if (currentTrack != null) showDetails = true }
                )

                PlaylistSection(
                    tracks = uiState.tracks,
                    currentTrackIndex = uiState.currentTrackIndex,
                    isPlaying = uiState.isPlaying,
                    onSelectTrack = onSelectTrack,
                    onChooseFolder = onChooseFolder,
                    modifier = Modifier.weight(1f)
                )

                PlaybackControls(
                    isPlaying = uiState.isPlaying,
                    hasTracks = hasTracks,
                    currentPosition = uiState.currentPosition,
                    bufferedPosition = uiState.bufferedPosition,
                    duration = uiState.duration,
                    isShuffleEnabled = uiState.isShuffleEnabled,
                    repeatMode = uiState.repeatMode,
                    playbackSpeed = uiState.playbackSpeed,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onStop = onStop,
                    onSeekTo = onSeekTo,
                    onToggleShuffle = onToggleShuffle,
                    onCycleRepeatMode = onCycleRepeatMode,
                    onCyclePlaybackSpeed = onCyclePlaybackSpeed
                )
            }
        }
    }
}

@Composable
private fun NowPlayingCard(
    currentTrack: AudioTrack?,
    isPlaying: Boolean,
    isLoading: Boolean,
    folderUri: Uri?,
    errorMessage: String?,
    onChooseFolder: () -> Unit,
    onShowDetails: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = currentTrack?.title ?: "Select a folder to start",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when {
                            isLoading -> "Building your playlist…"
                            currentTrack == null -> "Tap the folder icon to choose your music."
                            isPlaying -> "Now playing"
                            else -> "Ready to play"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onShowDetails, enabled = currentTrack != null) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Details")
                }
                TextButton(onClick = onChooseFolder) {
                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = if (folderUri == null) "Choose folder" else "Change folder")
                }
            }

            if (errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistSection(
    tracks: List<AudioTrack>,
    currentTrackIndex: Int,
    isPlaying: Boolean,
    onSelectTrack: (Int) -> Unit,
    onChooseFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Playlist",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (tracks.isNotEmpty()) {
                Text(
                    text = "${tracks.size} tracks",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (tracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
                contentAlignment = Alignment.Center
            ) {
                PlaylistEmptyState(onChooseFolder = onChooseFolder)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(tracks) { index, track ->
                    PlaylistItem(
                        index = index,
                        track = track,
                        isCurrent = index == currentTrackIndex,
                        isPlaying = isPlaying && index == currentTrackIndex,
                        onSelect = { onSelectTrack(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistItem(
    index: Int,
    track: AudioTrack,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onSelect: () -> Unit
) {
    val cardColor = if (isCurrent) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (isCurrent) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = cardColor,
        tonalElevation = if (isCurrent) 6.dp else 2.dp,
        shadowElevation = if (isCurrent) 6.dp else 0.dp,
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .sizeIn(minWidth = 42.dp)
                    .clip(CircleShape)
                    .background(
                        color = if (isCurrent) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (index + 1).toString().padStart(2, '0'),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isCurrent) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.uri.lastPathSegment ?: track.uri.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCurrent) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isCurrent) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.GraphicEq else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun PlaylistEmptyState(onChooseFolder: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.LibraryMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Your playlist is waiting",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Pick a folder with audio files and we’ll build a playlist automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            FilledTonalButton(onClick = onChooseFolder) {
                Text(text = "Browse folders")
            }
        }
    }
}

@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    hasTracks: Boolean,
    currentPosition: Long,
    bufferedPosition: Long,
    duration: Long,
    isShuffleEnabled: Boolean,
    repeatMode: Int,
    playbackSpeed: Float,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onStop: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeatMode: () -> Unit,
    onCyclePlaybackSpeed: () -> Unit
) {
    val safeDuration = duration.takeIf { it > 0L } ?: 0L
    val sliderRange = if (safeDuration > 0L) safeDuration.toFloat() else 1f
    var sliderPosition by remember(safeDuration, hasTracks) {
        mutableStateOf(currentPosition.coerceIn(0L, safeDuration).toFloat())
    }
    var isScrubbing by remember { mutableStateOf(false) }

    LaunchedEffect(currentPosition, safeDuration, hasTracks) {
        if (!isScrubbing) {
            sliderPosition = currentPosition.coerceIn(0L, safeDuration).toFloat()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Slider(
            value = sliderPosition,
            onValueChange = { value ->
                if (hasTracks && safeDuration > 0L) {
                    isScrubbing = true
                    sliderPosition = value.coerceIn(0f, sliderRange)
                }
            },
            onValueChangeFinished = {
                isScrubbing = false
                if (hasTracks && safeDuration > 0L) {
                    onSeekTo(sliderPosition.toLong())
                }
            },
            enabled = hasTracks,
            valueRange = 0f..sliderRange,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(sliderPosition.toLong()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (safeDuration > 0L) formatTime(safeDuration) else "--:--",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val shuffleColors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (isShuffleEnabled) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    contentColor = if (isShuffleEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                FilledTonalIconButton(
                    onClick = onToggleShuffle,
                    enabled = hasTracks,
                    colors = shuffleColors
                ) {
                    Icon(imageVector = Icons.Rounded.Shuffle, contentDescription = "Toggle shuffle")
                }

                val repeatSelected = repeatMode != Player.REPEAT_MODE_OFF
                val repeatColors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (repeatSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    contentColor = if (repeatSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                val repeatIcon = when (repeatMode) {
                    Player.REPEAT_MODE_ONE -> Icons.Rounded.RepeatOne
                    else -> Icons.Rounded.Repeat
                }
                FilledTonalIconButton(
                    onClick = onCycleRepeatMode,
                    enabled = hasTracks,
                    colors = repeatColors
                ) {
                    Icon(imageVector = repeatIcon, contentDescription = "Cycle repeat mode")
                }

                val speedLabel = String.format(Locale.getDefault(), "%.1fx", playbackSpeed)
                TextButton(onClick = onCyclePlaybackSpeed, enabled = hasTracks) {
                    Icon(
                        imageVector = Icons.Rounded.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = speedLabel)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(onClick = onPrevious, enabled = hasTracks) {
                    Icon(imageVector = Icons.Rounded.SkipPrevious, contentDescription = "Previous")
                }
                FilledIconButton(
                    onClick = { if (hasTracks) onPlayPause() },
                    enabled = hasTracks,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play"
                    )
                }
                FilledTonalIconButton(onClick = onNext, enabled = hasTracks) {
                    Icon(imageVector = Icons.Rounded.SkipNext, contentDescription = "Next")
                }
                FilledTonalIconButton(onClick = onStop, enabled = hasTracks) {
                    Icon(imageVector = Icons.Rounded.Stop, contentDescription = "Stop")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackDetailsSheet(
    track: AudioTrack,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Track details",
                style = MaterialTheme.typography.titleLarge
            )
            DetailRow(label = "Title", value = track.title)
            track.artist?.let { artist ->
                DetailRow(label = "Artist", value = artist)
            }
            track.album?.let { album ->
                DetailRow(label = "Album", value = album)
            }
            track.durationMs?.let { duration ->
                DetailRow(label = "Duration", value = formatTime(duration))
            }
            track.fileSizeBytes?.let { size ->
                formatFileSize(size)?.let { readable ->
                    DetailRow(label = "File size", value = readable)
                }
            }
            DetailRow(label = "Source", value = track.uri.toString())
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
    }
}

private fun formatTime(positionMs: Long): String {
    if (positionMs <= 0L) return "0:00"
    val totalSeconds = positionMs / 1000
    val seconds = (totalSeconds % 60).toInt()
    val minutes = ((totalSeconds / 60) % 60).toInt()
    val hours = (totalSeconds / 3600).toInt()
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%d:%02d", totalSeconds / 60, seconds)
    }
}

private fun formatFileSize(sizeBytes: Long): String? {
    if (sizeBytes <= 0L) return null
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = sizeBytes.toDouble()
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.lastIndex) {
        value /= 1024
        unitIndex++
    }
    return String.format(Locale.getDefault(), "%.1f %s", value, units[unitIndex])
}

@Preview(showBackground = true)
@Composable
private fun AudioPlayerScreenPreview() {
    AudioplayerTheme {
        AudioPlayerScreen(
            uiState = PlayerUiState(
                folderUri = Uri.parse("content://demo/music"),
                tracks = listOf(
                    AudioTrack(
                        title = "Lo-fi Vibes",
                        uri = Uri.parse("content://demo/lofi"),
                        artist = "Loft Beats",
                        album = "Late Night",
                        durationMs = 180_000L,
                        fileSizeBytes = 4_200_000L
                    ),
                    AudioTrack(
                        title = "Ocean Echoes",
                        uri = Uri.parse("content://demo/ocean"),
                        artist = "Tide",
                        album = "Blue",
                        durationMs = 200_000L,
                        fileSizeBytes = 4_600_000L
                    ),
                    AudioTrack(
                        title = "Night Walk",
                        uri = Uri.parse("content://demo/night"),
                        artist = "City Lights",
                        album = "Midnight",
                        durationMs = 220_000L,
                        fileSizeBytes = 5_000_000L
                    )
                ),
                currentTrackIndex = 1,
                isPlaying = true,
                currentPosition = 90_000L,
                bufferedPosition = 120_000L,
                duration = 240_000L,
                isShuffleEnabled = true,
                repeatMode = Player.REPEAT_MODE_ALL,
                playbackSpeed = 1.25f
            ),
            onChooseFolder = {},
            onPlayPause = {},
            onNext = {},
            onPrevious = {},
            onStop = {},
            onSelectTrack = {},
            onSeekTo = {},
            onToggleShuffle = {},
            onCycleRepeatMode = {},
            onCyclePlaybackSpeed = {}
        )
    }
}
