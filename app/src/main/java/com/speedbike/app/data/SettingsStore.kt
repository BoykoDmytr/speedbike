package com.speedbike.app.data

import android.content.Context

/** Persists user preferences across launches. */
class SettingsStore(context: Context) {
    private val prefs =
        context.getSharedPreferences("speedbike_settings", Context.MODE_PRIVATE)

    var targetDistanceKm: Double
        get() = prefs.getFloat(KEY_TARGET, 10f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_TARGET, value.toFloat()).apply()

    var alarmEnabled: Boolean
        get() = prefs.getBoolean(KEY_ALARM, true)
        set(value) = prefs.edit().putBoolean(KEY_ALARM, value).apply()

    var repeatAlarm: Boolean
        get() = prefs.getBoolean(KEY_REPEAT, false)
        set(value) = prefs.edit().putBoolean(KEY_REPEAT, value).apply()

    var useMiles: Boolean
        get() = prefs.getBoolean(KEY_MILES, false)
        set(value) = prefs.edit().putBoolean(KEY_MILES, value).apply()

    var voiceEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOICE, false)
        set(value) = prefs.edit().putBoolean(KEY_VOICE, value).apply()

    var keepScreenOn: Boolean
        get() = prefs.getBoolean(KEY_KEEP_SCREEN, true)
        set(value) = prefs.edit().putBoolean(KEY_KEEP_SCREEN, value).apply()

    var autoPause: Boolean
        get() = prefs.getBoolean(KEY_AUTO_PAUSE, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_PAUSE, value).apply()

    var weightKg: Double
        get() = prefs.getFloat(KEY_WEIGHT, 70f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_WEIGHT, value.toFloat()).apply()

    /** The map tile source key (see [com.speedbike.app.ui.components.MapStyle]). */
    var mapStyle: String
        get() = prefs.getString(KEY_MAP_STYLE, "standard") ?: "standard"
        set(value) = prefs.edit().putString(KEY_MAP_STYLE, value).apply()

    private companion object {
        const val KEY_TARGET = "target_km"
        const val KEY_ALARM = "alarm_enabled"
        const val KEY_REPEAT = "repeat_alarm"
        const val KEY_MILES = "use_miles"
        const val KEY_VOICE = "voice_enabled"
        const val KEY_KEEP_SCREEN = "keep_screen_on"
        const val KEY_AUTO_PAUSE = "auto_pause"
        const val KEY_WEIGHT = "weight_kg"
        const val KEY_MAP_STYLE = "map_style"
    }
}
