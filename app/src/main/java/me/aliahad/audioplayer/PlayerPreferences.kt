package me.aliahad.audioplayer

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.media3.common.Player
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.playerPreferencesDataStore by preferencesDataStore(name = "player_preferences")

data class PlayerPreferencesData(
    val folderUri: String? = null,
    val currentTrackUri: String? = null,
    val positionMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val playbackSpeed: Float = 1f
)

class PlayerPreferences(context: Context) {

    private val dataStore = context.playerPreferencesDataStore

    val preferencesFlow: Flow<PlayerPreferencesData> = dataStore.data.map { preferences ->
        PlayerPreferencesData(
            folderUri = preferences[FOLDER_URI_KEY],
            currentTrackUri = preferences[CURRENT_TRACK_URI_KEY],
            positionMs = preferences[POSITION_MS_KEY] ?: 0L,
            shuffleEnabled = preferences[SHUFFLE_ENABLED_KEY] ?: false,
            repeatMode = preferences[REPEAT_MODE_KEY] ?: Player.REPEAT_MODE_OFF,
            playbackSpeed = preferences[PLAYBACK_SPEED_KEY] ?: 1f
        )
    }

    suspend fun getPreferences(): PlayerPreferencesData = preferencesFlow.first()

    suspend fun saveState(
        folderUri: Uri?,
        trackUri: Uri?,
        positionMs: Long,
        shuffleEnabled: Boolean,
        repeatMode: Int,
        playbackSpeed: Float
    ) {
        dataStore.edit { preferences ->
            if (folderUri == null) {
                preferences.remove(FOLDER_URI_KEY)
            } else {
                preferences[FOLDER_URI_KEY] = folderUri.toString()
            }
            if (trackUri == null) {
                preferences.remove(CURRENT_TRACK_URI_KEY)
            } else {
                preferences[CURRENT_TRACK_URI_KEY] = trackUri.toString()
            }
            preferences[POSITION_MS_KEY] = positionMs
            preferences[SHUFFLE_ENABLED_KEY] = shuffleEnabled
            preferences[REPEAT_MODE_KEY] = repeatMode
            preferences[PLAYBACK_SPEED_KEY] = playbackSpeed
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private companion object {
        val FOLDER_URI_KEY = stringPreferencesKey("folder_uri")
        val CURRENT_TRACK_URI_KEY = stringPreferencesKey("current_track_uri")
        val POSITION_MS_KEY = longPreferencesKey("position_ms")
        val SHUFFLE_ENABLED_KEY = booleanPreferencesKey("shuffle_enabled")
        val REPEAT_MODE_KEY = intPreferencesKey("repeat_mode")
        val PLAYBACK_SPEED_KEY = floatPreferencesKey("playback_speed")
    }
}
