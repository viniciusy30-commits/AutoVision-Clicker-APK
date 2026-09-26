package com.autovision.clicker.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.autovision.clicker.models.AutomationConfig
import com.autovision.clicker.models.RecognitionMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("autovision_settings")

class AppSettings(private val context: Context) {
    private object Keys {
        val interval = longPreferencesKey("interval")
        val repetitions = intPreferencesKey("repetitions")
        val confidence = doublePreferencesKey("confidence")
        val analysisInterval = longPreferencesKey("analysis_interval")
        val debug = booleanPreferencesKey("visual_debug")
        val mode = androidx.datastore.preferences.core.stringPreferencesKey("mode")
    }

    val config: Flow<AutomationConfig> = context.dataStore.data.map { p ->
        AutomationConfig(
            intervalMs = p[Keys.interval] ?: 250,
            repetitions = p[Keys.repetitions] ?: 1,
            minConfidence = p[Keys.confidence] ?: .80,
            analysisIntervalMs = p[Keys.analysisInterval] ?: 250,
            mode = runCatching {
                RecognitionMode.valueOf(p[Keys.mode] ?: RecognitionMode.HYBRID.name)
            }.getOrDefault(RecognitionMode.HYBRID)
        )
    }

    val visualDebug: Flow<Boolean> = context.dataStore.data.map { it[Keys.debug] ?: true }

    suspend fun saveConfig(config: AutomationConfig) {
        context.dataStore.edit { p ->
            p[Keys.interval] = config.intervalMs
            p[Keys.repetitions] = config.repetitions
            p[Keys.confidence] = config.minConfidence
            p[Keys.analysisInterval] = config.analysisIntervalMs
            p[Keys.mode] = config.mode.name
        }
    }

    suspend fun setVisualDebug(enabled: Boolean) {
        context.dataStore.edit { it[Keys.debug] = enabled }
    }
}