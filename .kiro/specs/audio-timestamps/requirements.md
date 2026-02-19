# Requirements Document

## Introduction

Audio Timestamps is a bookmarking feature for the Audio Player app. It allows the user to create timestamped bookmarks at any point during audio playback, optionally attach a note, and later tap a bookmark to resume playback from that exact position. Timestamps are scoped per audio file within a folder and persisted using a Room (SQLite) database, which is the recommended approach for structured, queryable collections of records. DataStore is unsuitable here because it is designed for flat key-value preferences, not relational data with one-to-many relationships (folder → tracks → timestamps).

## Glossary

- **Timestamp_Bookmark**: A persisted record containing the playback position (in milliseconds), an optional text note, the audio file URI, the folder URI, and a creation date.
- **Timestamp_Database**: A Room (SQLite) database that stores all Timestamp_Bookmark records locally on the device.
- **Timestamp_DAO**: The Room Data Access Object that provides type-safe queries for creating, reading, and deleting Timestamp_Bookmark records.
- **Bookmark_Button**: A UI control displayed during playback that captures the current playback position and opens the Bookmark_Dialog.
- **Bookmark_Dialog**: A modal dialog that allows the user to optionally enter a note before saving a Timestamp_Bookmark.
- **Timestamp_List**: A scrollable UI section displayed below the player controls showing all Timestamp_Bookmark records for the currently playing audio file, ordered by playback position.
- **Audio_Player**: The existing audio playback application.
- **ViewModel**: The AudioPlayerViewModel that orchestrates UI state and player control.

## Requirements

### Requirement 1: Create a Timestamp Bookmark

**User Story:** As a user, I want to create a bookmark at the current playback position, so that I can mark interesting moments or stopping points in long audio files.

#### Acceptance Criteria

1. WHILE an audio track is loaded, THE Audio_Player SHALL display the Bookmark_Button in the player controls area.
2. WHEN the user taps the Bookmark_Button, THE Audio_Player SHALL open the Bookmark_Dialog pre-filled with the current playback position in milliseconds.
3. WHEN the user taps the Bookmark_Button, THE Audio_Player SHALL capture the playback position at the moment of the tap, regardless of whether playback continues during dialog interaction.


### Requirement 2: Bookmark Dialog with Optional Note

**User Story:** As a user, I want to optionally write a note when creating a bookmark, so that I can remember why I marked that moment.

#### Acceptance Criteria

1. THE Bookmark_Dialog SHALL display the captured playback position formatted as HH:MM:SS or MM:SS.
2. THE Bookmark_Dialog SHALL provide a text input field for an optional note.
3. THE Bookmark_Dialog SHALL provide a submit button to save the Timestamp_Bookmark.
4. THE Bookmark_Dialog SHALL provide a cancel/dismiss button to discard the bookmark without saving.
5. WHEN the user submits the Bookmark_Dialog with an empty note field, THE Audio_Player SHALL save the Timestamp_Bookmark with a null note value.
6. WHEN the user submits the Bookmark_Dialog with a non-empty note, THE Audio_Player SHALL save the Timestamp_Bookmark with the provided note text.

### Requirement 3: Persist Timestamps with Room Database

**User Story:** As a user, I want my bookmarks to be saved permanently, so that I can access them across app restarts.

#### Acceptance Criteria

1. THE Timestamp_Database SHALL store each Timestamp_Bookmark with the following fields: a unique identifier, the audio file URI, the folder URI, the playback position in milliseconds, an optional note, and a creation timestamp.
2. THE Timestamp_Database SHALL enforce a composite association between the audio file URI and the folder URI so that bookmarks are scoped per audio file within a specific folder.
3. WHEN the user saves a Timestamp_Bookmark, THE Timestamp_DAO SHALL insert the record into the Timestamp_Database.
4. WHEN the app restarts, THE Timestamp_Database SHALL retain all previously saved Timestamp_Bookmark records.

### Requirement 4: Display Timestamps for Current Track

**User Story:** As a user, I want to see all my bookmarks for the currently playing audio file, so that I can quickly navigate to marked positions.

#### Acceptance Criteria

1. WHILE an audio track is loaded, THE Audio_Player SHALL display the Timestamp_List below the player controls.
2. THE Timestamp_List SHALL show only Timestamp_Bookmark records belonging to the currently loaded audio file within the current folder.
3. THE Timestamp_List SHALL order Timestamp_Bookmark records by playback position in ascending order.
4. THE Timestamp_List SHALL display each Timestamp_Bookmark with the formatted playback position (HH:MM:SS or MM:SS) and the note text if present.
5. WHEN the currently loaded audio track changes, THE Timestamp_List SHALL update to show Timestamp_Bookmark records for the new track.
6. IF no Timestamp_Bookmark records exist for the current track, THEN THE Timestamp_List SHALL display an empty state or remain hidden.


### Requirement 5: Seek to Timestamp Position

**User Story:** As a user, I want to tap on a bookmark time to jump to that position in the audio, so that I can resume listening from where I left off.

#### Acceptance Criteria

1. WHEN the user taps a Timestamp_Bookmark time in the Timestamp_List, THE Audio_Player SHALL seek playback to the position stored in that Timestamp_Bookmark.
2. WHEN the user taps a Timestamp_Bookmark time while playback is paused, THE Audio_Player SHALL seek to the position and begin playback.
3. WHEN the user taps a Timestamp_Bookmark time while playback is active, THE Audio_Player SHALL seek to the position and continue playback.

### Requirement 6: Delete a Timestamp Bookmark

**User Story:** As a user, I want to remove bookmarks I no longer need, so that my bookmark list stays clean and relevant.

#### Acceptance Criteria

1. THE Timestamp_List SHALL provide a delete action for each Timestamp_Bookmark entry.
2. WHEN the user triggers the delete action on a Timestamp_Bookmark, THE Timestamp_DAO SHALL remove that record from the Timestamp_Database.
3. WHEN a Timestamp_Bookmark is deleted, THE Timestamp_List SHALL immediately reflect the removal without requiring a manual refresh.

### Requirement 7: Folder-Scoped Timestamp Isolation

**User Story:** As a user, I want bookmarks to be tied to the specific folder and audio file they were created for, so that switching folders does not mix up my bookmarks.

#### Acceptance Criteria

1. THE Timestamp_Database SHALL associate each Timestamp_Bookmark with both the audio file URI and the folder URI.
2. WHEN the user selects a different folder, THE Timestamp_List SHALL display only Timestamp_Bookmark records associated with audio files in the newly selected folder.
3. WHEN two audio files in different folders share the same file name, THE Timestamp_Database SHALL maintain separate Timestamp_Bookmark records for each file based on the unique audio file URI.

### Requirement 8: Haptic Feedback on Bookmark Creation

**User Story:** As a user, I want tactile confirmation when I create a bookmark, so that I know the action was registered without looking at the screen.

#### Acceptance Criteria

1. WHEN the user taps the Bookmark_Button, THE Audio_Player SHALL trigger a short haptic feedback vibration, consistent with the existing haptic feedback pattern used in the app.

### Requirement 9: Timestamp Position Formatting

**User Story:** As a user, I want bookmark times displayed in a human-readable format, so that I can quickly understand where each bookmark is in the audio.

#### Acceptance Criteria

1. WHEN the playback position is less than one hour, THE Audio_Player SHALL format the Timestamp_Bookmark position as MM:SS.
2. WHEN the playback position is one hour or greater, THE Audio_Player SHALL format the Timestamp_Bookmark position as HH:MM:SS.
3. FOR ALL valid Timestamp_Bookmark positions, formatting then parsing the formatted string SHALL produce the original position in milliseconds (round-trip property, accounting for millisecond truncation to seconds).
