package com.autovision.clicker.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.autovision.clicker.models.AutomationConfig
import com.autovision.clicker.models.AutomationProfile
import com.autovision.clicker.models.RecognitionMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Context.profileDataStore by preferencesDataStore("autovision_profiles")

class ProfileStore(private val context: Context) {
    private val profilesKey = stringPreferencesKey("profiles_json")

    val profiles: Flow<List<AutomationProfile>> = context.profileDataStore.data.map { preferences ->
        decode(preferences[profilesKey].orEmpty())
    }

    suspend fun save(profile: AutomationProfile) {
        context.profileDataStore.edit { preferences ->
            val current = decode(preferences[profilesKey].orEmpty())
                .filterNot { it.id == profile.id } + profile
            preferences[profilesKey] = encode(current)
        }
    }

    suspend fun delete(id: String) {
        context.profileDataStore.edit { preferences ->
            preferences[profilesKey] = encode(decode(preferences[profilesKey].orEmpty()).filterNot { it.id == id })
        }
    }

    suspend fun duplicate(profile: AutomationProfile) {
        save(profile.copy(id = UUID.randomUUID().toString(), name = "${profile.name} (cópia)"))
    }

    private fun encode(profiles: List<AutomationProfile>): String = JSONArray().apply {
        profiles.forEach { profile ->
            put(JSONObject().apply {
                put("id", profile.id)
                put("name", profile.name)
                put("interval", profile.config.intervalMs)
                put("repetitions", profile.config.repetitions)
                put("confidence", profile.config.minConfidence)
                put("analysisInterval", profile.config.analysisIntervalMs)
                put("mode", profile.config.mode.name)
            })
        }
    }.toString()

    private fun decode(value: String): List<AutomationProfile> {
        if (value.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(value)
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                AutomationProfile(
                    id = item.getString("id"),
                    name = item.getString("name"),
                    config = AutomationConfig(
                        intervalMs = item.optLong("interval", 250),
                        repetitions = item.optInt("repetitions", 1),
                        minConfidence = item.optDouble("confidence", .80),
                        analysisIntervalMs = item.optLong("analysisInterval", 250),
                        mode = runCatching {
                            RecognitionMode.valueOf(item.optString("mode", RecognitionMode.HYBRID.name))
                        }.getOrDefault(RecognitionMode.HYBRID)
                    )
                )
            }
        }.getOrDefault(emptyList())
    }
}