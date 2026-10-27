package com.example.voicenotes.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    val sampleRate: Flow<Int> = context.settingsDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> preferences[SAMPLE_RATE] ?: DEFAULT_SAMPLE_RATE }

    suspend fun setSampleRate(rate: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[SAMPLE_RATE] = rate
        }
    }

    companion object {
        const val DEFAULT_SAMPLE_RATE = 16_000
        const val HIGH_SAMPLE_RATE = 44_100
        private val SAMPLE_RATE = intPreferencesKey("sample_rate")
    }
}
