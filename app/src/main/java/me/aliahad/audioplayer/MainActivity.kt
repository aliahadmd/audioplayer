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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.aliahad.audioplayer.ui.theme.AudioplayerTheme

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
                    onSelectTrack = { index -> viewModel.selectTrack(index) }
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
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(text = "Audio Player") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = onChooseFolder) {
                Text(text = "Choose Folder")
            }

            if (uiState.folderUri != null) {
                Text(
                    text = "Current folder: ${uiState.folderUri}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            when {
                uiState.isLoading -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(text = "Scanning folder for audio files…")
                    }
                }

                uiState.tracks.isEmpty() -> {
                    Text(
                        text = uiState.errorMessage ?: "Select a folder to build a playlist.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(uiState.tracks) { index, track ->
                            val isCurrent = index == uiState.currentTrackIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = if (isCurrent) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    )
                                    .clickable { onSelectTrack(index) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (isCurrent) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    Text(
                                        text = track.uri.lastPathSegment ?: track.uri.toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (isCurrent) {
                                    Text(
                                        text = if (uiState.isPlaying) "Playing" else "Ready",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            ControlRow(
                isPlaying = uiState.isPlaying,
                enabled = uiState.tracks.isNotEmpty() && !uiState.isLoading,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onPrevious = onPrevious,
                onStop = onStop
            )
        }
    }
}

@Composable
private fun ControlRow(
    isPlaying: Boolean,
    enabled: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onStop: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(onClick = onPrevious, enabled = enabled) {
            Text(text = "Previous")
        }
        Button(onClick = onPlayPause, enabled = enabled) {
            Text(text = if (isPlaying) "Pause" else "Play")
        }
        Button(onClick = onStop, enabled = enabled) {
            Text(text = "Stop")
        }
        Button(onClick = onNext, enabled = enabled) {
            Text(text = "Next")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AudioPlayerScreenPreview() {
    AudioplayerTheme {
        AudioPlayerScreen(
            uiState = PlayerUiState(
                tracks = listOf(
                    AudioTrack("Track One", Uri.parse("content://demo/track1")),
                    AudioTrack("Track Two", Uri.parse("content://demo/track2"))
                ),
                currentTrackIndex = 0,
                isPlaying = true
            ),
            onChooseFolder = {},
            onPlayPause = {},
            onNext = {},
            onPrevious = {},
            onStop = {},
            onSelectTrack = {}
        )
    }
}
