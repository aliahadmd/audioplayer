# Implementation Plan: Audio Timestamps

## Overview

Bottom-up implementation: add Room dependencies, create the persistence layer (entity, DAO, database), build the pure formatting utilities, integrate into the ViewModel, then wire up the Compose UI. Property-based and unit tests are interleaved close to the code they validate.

## Tasks

- [x] 1. Add Room and KSP dependencies
  - [x] 1.1 Add Room and KSP version entries to `gradle/libs.versions.toml`
    - Add `room` version (e.g. `2.6.1`) and `ksp` version compatible with Kotlin 2.2.10
    - Add library declarations: `room-runtime`, `room-ktx`, `room-compiler`
    - Add KSP plugin declaration
    - _Requirements: 3.1_

  - [x] 1.2 Update `app/build.gradle.kts` with Room dependencies
    - Apply the KSP plugin
    - Add `implementation` for `room-runtime` and `room-ktx`
    - Add `ksp` for `room-compiler`
    - _Requirements: 3.1_

- [x] 2. Create Room persistence layer
  - [x] 2.1 Create `TimestampBookmark.kt` with entity, DAO, and database
    - Create `app/src/main/java/me/aliahad/audioplayer/TimestampBookmark.kt`
    - Define `TimestampBookmark` entity with fields: `id` (auto-generate PK), `audioFileUri`, `folderUri`, `positionMs`, `note` (nullable), `createdAt`
    - Define `TimestampDao` interface with `getBookmarksForTrack(audioFileUri, folderUri): Flow<List<TimestampBookmark>>`, `insert(bookmark)`, `deleteById(id)`
    - Define `TimestampDatabase` with companion object singleton (`getInstance(context)`) using double-checked locking
    - _Requirements: 3.1, 3.2, 3.3, 7.1_

- [x] 3. Create timestamp formatting utilities
  - [x] 3.1 Create `TimestampFormatter.kt` with `formatTimestamp()` and `parseTimestamp()`
    - Create `app/src/main/java/me/aliahad/audioplayer/TimestampFormatter.kt`
    - `formatTimestamp(positionMs: Long): String` — returns `MM:SS` for < 1 hour, `HH:MM:SS` for >= 1 hour
    - `parseTimestamp(formatted: String): Long` — parses formatted string back to milliseconds
    - Use `Locale.ROOT` for formatting to avoid locale issues
    - _Requirements: 9.1, 9.2, 9.3_

  - [x] 3.2 Write property test for format/parse round-trip
    - **Property: Format/parse round-trip consistency**
    - For any non-negative `positionMs`, `parseTimestamp(formatTimestamp(positionMs))` equals `positionMs` truncated to whole seconds (i.e., `(positionMs / 1000) * 1000`)
    - Create `app/src/test/java/me/aliahad/audioplayer/TimestampFormatterPropertyTest.kt`
    - Use Kotest `FunSpec` + `forAll` with `Arb.long(0..359999000)` (up to ~100 hours)
    - **Validates: Requirement 9.3**

  - [x] 3.3 Write unit tests for timestamp formatting specific values
    - Create `app/src/test/java/me/aliahad/audioplayer/TimestampFormatterUnitTest.kt`
    - Test `0ms → "0:00"`, `59999ms → "0:59"`, `60000ms → "1:00"`, `3599999ms → "59:59"`, `3600000ms → "1:00:00"`
    - Test `parseTimestamp` with matching inputs
    - Test edge case: `parseTimestamp` with invalid format throws `IllegalArgumentException`
    - **Validates: Requirements 9.1, 9.2**

- [x] 4. Checkpoint — Verify persistence layer and formatter
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Integrate timestamps into ViewModel
  - [x] 5.1 Extend `PlayerUiState` and add bookmark state fields
    - Add `timestamps: List<TimestampBookmark> = emptyList()` to `PlayerUiState`
    - Add `bookmarkDialogPositionMs: Long? = null` to `PlayerUiState` (non-null when dialog is showing)
    - _Requirements: 4.1, 2.1_

  - [x] 5.2 Add DAO initialization and Flow collection in `AudioPlayerViewModel`
    - Initialize `TimestampDatabase.getInstance(context)` and get `TimestampDao` reference
    - On track change, use `flatMapLatest` to switch to the new track's bookmarks Flow filtered by `(audioFileUri, folderUri)`
    - Collect bookmarks into `PlayerUiState.timestamps`
    - _Requirements: 4.2, 4.3, 4.5, 7.2_

  - [x] 5.3 Add bookmark CRUD and seek functions to ViewModel
    - `onBookmarkTap()` — capture `player.currentPosition`, set `bookmarkDialogPositionMs` in state
    - `saveBookmark(positionMs: Long, note: String?)` — insert via DAO, clear dialog state
    - `dismissBookmarkDialog()` — clear `bookmarkDialogPositionMs`
    - `deleteBookmark(id: Long)` — delete via DAO
    - `seekToTimestamp(positionMs: Long)` — call `player.seekTo(positionMs)`, start playback if paused
    - _Requirements: 1.2, 1.3, 2.3, 2.4, 2.5, 2.6, 5.1, 5.2, 5.3, 6.2_

  - [x] 5.4 Write property test for bookmark ordering
    - **Property: Bookmarks are always ordered by positionMs ascending**
    - For any list of `TimestampBookmark` with the same `(audioFileUri, folderUri)`, the DAO query returns them sorted by `positionMs ASC`
    - Since this requires Room, test the ordering invariant on the data class list: given any shuffled list, sorting by `positionMs` produces a list where each element's `positionMs` <= the next
    - Create `app/src/test/java/me/aliahad/audioplayer/TimestampBookmarkPropertyTest.kt`
    - Use Kotest `forAll` with generated lists of `TimestampBookmark`
    - **Validates: Requirement 4.3**

  - [x] 5.5 Write property test for folder isolation
    - **Property: Bookmarks are isolated by (audioFileUri, folderUri) pair**
    - For any two distinct `(audioFileUri, folderUri)` pairs, filtering a mixed list by one pair never returns bookmarks belonging to the other pair
    - Add to `TimestampBookmarkPropertyTest.kt`
    - Use Kotest `forAll` with generated bookmark lists and two distinct URI pairs
    - **Validates: Requirements 7.1, 7.2, 7.3**

  - [x] 5.6 Write property test for bookmark dialog toggle state
    - **Property: Dialog state toggles correctly**
    - `bookmarkDialogPositionMs` is non-null after `onBookmarkTap()` and null after `dismissBookmarkDialog()` or `saveBookmark()`
    - Test as a state-machine property: for any sequence of tap/dismiss/save actions, the dialog state is consistent
    - Add to `TimestampBookmarkPropertyTest.kt`
    - **Validates: Requirements 1.2, 2.3, 2.4**

- [x] 6. Checkpoint — Verify ViewModel integration
  - Ensure all tests pass, ask the user if questions arise.

- [x] 7. Build Compose UI components
  - [x] 7.1 Add `BookmarkButton` composable to playback controls in `MainActivity.kt`
    - Add a bookmark icon button in the player controls area, enabled when a track is loaded
    - On tap: trigger haptic feedback and call `onBookmarkTap()`
    - _Requirements: 1.1, 1.2, 8.1_

  - [x] 7.2 Add `BookmarkDialog` composable in `MainActivity.kt`
    - Show when `bookmarkDialogPositionMs != null`
    - Display formatted position using `formatTimestamp()`
    - Include `OutlinedTextField` for optional note
    - Save button calls `onSaveBookmark(positionMs, note)` (pass null if note is blank)
    - Cancel button calls `onDismissBookmarkDialog()`
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_

  - [x] 7.3 Add `TimestampListSection` composable in `MainActivity.kt`
    - Display below player controls, showing bookmarks for current track
    - Each entry: formatted time (tappable, calls `onSeekToTimestamp`), optional note text, delete icon button (calls `onDeleteTimestamp`)
    - Hide section or show empty state when no bookmarks exist
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.6, 5.1, 6.1, 6.3_

  - [x] 7.4 Wire new callbacks through `AudioPlayerScreen`
    - Add parameters: `onBookmarkTap`, `onSaveBookmark`, `onDismissBookmarkDialog`, `onSeekToTimestamp`, `onDeleteTimestamp`
    - Connect to ViewModel functions in `MainActivity`
    - _Requirements: 1.1, 5.1, 6.1_

- [x] 8. Final checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Kotest `FunSpec` with `forAll` is used for property tests, matching existing project test style
- Run tests with `./gradlew testDebugUnitTest`
- Property tests validate universal correctness properties; unit tests validate specific examples and edge cases
