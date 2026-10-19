package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "watersort_prefs")

/**
 * Kyun: Level aur sound mute preference ko DataStore me persistently store karta hai.
 */
class PreferencesManager(private val context: Context) {

    companion object {
        private val KEY_SAVED_LEVEL = intPreferencesKey("ws_level")
        // Kyun: User requirement C.15 — Mute setting ke liye DataStore key
        private val KEY_MUTED = booleanPreferencesKey("ws_muted")
    }

    val savedLevelFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_SAVED_LEVEL] ?: 1
    }

    val isMutedFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_MUTED] ?: false
    }

    suspend fun saveLevel(level: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SAVED_LEVEL] = level
        }
    }

    suspend fun setMuted(muted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MUTED] = muted
        }
    }
}
