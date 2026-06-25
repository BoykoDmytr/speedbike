package com.speedbike.app.data

import android.content.Context

/** Persists the user's target distance and alarm preference across launches. */
class SettingsStore(context: Context) {
    private val prefs =
        context.getSharedPreferences("speedbike_settings", Context.MODE_PRIVATE)

    var targetDistanceKm: Double
        get() = prefs.getFloat(KEY_TARGET, 10f).toDouble()
        set(value) {
            prefs.edit().putFloat(KEY_TARGET, value.toFloat()).apply()
        }

    var alarmEnabled: Boolean
        get() = prefs.getBoolean(KEY_ALARM, true)
        set(value) {
            prefs.edit().putBoolean(KEY_ALARM, value).apply()
        }

    private companion object {
        const val KEY_TARGET = "target_km"
        const val KEY_ALARM = "alarm_enabled"
    }
}
