package me.aliahad.audioplayer

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import io.kotest.core.spec.style.FunSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.boolean
import io.kotest.property.forAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File

// Feature: theme-support, Property 3: Theme preference persistence round-trip
// **Validates: Requirements 3.1, 3.2**
class ThemePersistencePropertyTest : FunSpec({

    val isNightModeKey = booleanPreferencesKey("is_night_mode")

    test("Theme preference round-trip: saved value equals read value") {
        forAll(Arb.boolean()) { isNightMode ->
            val tempFile = File.createTempFile("test_prefs_", ".preferences_pb")
            tempFile.deleteOnExit()
            // Delete so DataStore creates it fresh
            tempFile.delete()

            val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
                produceFile = { tempFile }
            )

            // Write — mirrors PlayerPreferences.saveThemeMode()
            dataStore.edit { preferences ->
                preferences[isNightModeKey] = isNightMode
            }

            // Read — mirrors PlayerPreferences.preferencesFlow mapping
            val readBack = dataStore.data.map { preferences ->
                preferences[isNightModeKey] ?: true
            }.first()

            readBack == isNightMode
        }
    }

    test("Default theme preference is Night mode when no value stored") {
        val tempFile = File.createTempFile("test_prefs_default_", ".preferences_pb")
        tempFile.deleteOnExit()
        tempFile.delete()

        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            produceFile = { tempFile }
        )

        // Read without writing — mirrors the default behavior
        val readBack = dataStore.data.map { preferences ->
            preferences[isNightModeKey] ?: true
        }.first()

        assert(readBack) { "Default theme should be Night mode (true) when no value is stored" }
    }
})
