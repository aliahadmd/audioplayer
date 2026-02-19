# Project Structure

Single-module Android app. All source lives under `app/src/main/java/me/aliahad/audioplayer/`.

```
app/src/main/java/me/aliahad/audioplayer/
├── MainActivity.kt          # Activity + all Compose UI (single-file UI)
├── AudioPlayerViewModel.kt  # AndroidViewModel, playlist logic, ExoPlayer control, state management
├── AudioPlayerManager.kt    # Singleton holder for ExoPlayer + MediaSession instances
├── AudioPlayerService.kt    # Foreground service for background playback & notification
├── PlayerPreferences.kt     # DataStore wrapper for persisting playback state
└── ui/theme/
    ├── Color.kt             # Color palette
    ├── Theme.kt             # Material 3 theme (dynamic color enabled)
    └── Type.kt              # Typography definitions
```

## Architecture Notes
- No DI framework — `AudioPlayerManager` is a manual singleton accessed via `getInstance(context)`.
- `AudioPlayerViewModel` is the central orchestrator: it owns the `ExoPlayer` reference, manages UI state via `StateFlow<PlayerUiState>`, handles folder scanning, and persists preferences.
- All Compose UI is in `MainActivity.kt` — composables are private functions, not split into separate files.
- Data classes `AudioTrack` and `PlayerUiState` are defined in `AudioPlayerViewModel.kt`.
- The service (`AudioPlayerService`) shares the player instance through `AudioPlayerManager` and manages the notification lifecycle.
- Folder scanning uses `DocumentFile` (Storage Access Framework) and `MediaMetadataRetriever` for metadata extraction, running on `Dispatchers.IO`.

## Resources
- `res/values/strings.xml` — app name and notification channel name
- `res/values/themes.xml` — base Android theme (Material Light NoActionBar)
- `res/xml/` — backup rules
- `res/drawable/` — adaptive icon vectors
