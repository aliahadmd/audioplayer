package me.aliahad.audioplayer

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.playerPreferencesDataStore by preferencesDataStore(name = "player_preferences")

data class PlayerPreferencesData(
    val folderUri: String? = null,
    val currentTrackUri: String? = null
)

class PlayerPreferences(context: Context) {

    private val dataStore = context.playerPreferencesDataStore

    val preferencesFlow: Flow<PlayerPreferencesData> = dataStore.data.map { preferences ->
        PlayerPreferencesData(
            folderUri = preferences[FOLDER_URI_KEY],
            currentTrackUri = preferences[CURRENT_TRACK_URI_KEY]
        )
    }

    suspend fun getPreferences(): PlayerPreferencesData = preferencesFlow.first()

    suspend fun setFolderUri(folderUri: Uri?) {
        dataStore.edit { preferences ->
            if (folderUri == null) {
                preferences.remove(FOLDER_URI_KEY)
            } else {
                preferences[FOLDER_URI_KEY] = folderUri.toString()
            }
        }
    }

    suspend fun setCurrentTrackUri(trackUri: Uri?) {
        dataStore.edit { preferences ->
            if (trackUri == null) {
                preferences.remove(CURRENT_TRACK_URI_KEY)
            } else {
                preferences[CURRENT_TRACK_URI_KEY] = trackUri.toString()
            }
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private companion object {
        val FOLDER_URI_KEY = stringPreferencesKey("folder_uri")
        val CURRENT_TRACK_URI_KEY = stringPreferencesKey("current_track_uri")
    }
}
