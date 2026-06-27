package com.speedbike.app.data.pet

import android.content.Context
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Persists the whole [PetState] as a single JSON blob in SharedPreferences. */
class PetStore(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences("speedbike_pet", Context.MODE_PRIVATE)

    fun load(): PetState {
        val raw = prefs.getString(KEY, null) ?: return PetState()
        return runCatching { json.decodeFromString<PetState>(raw) }.getOrDefault(PetState())
    }

    fun save(state: PetState) {
        prefs.edit().putString(KEY, json.encodeToString(state)).apply()
    }

    private companion object {
        const val KEY = "pet_state"
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}
