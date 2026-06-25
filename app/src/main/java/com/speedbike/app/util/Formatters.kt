package com.speedbike.app.util

import java.util.Locale
import kotlin.math.roundToInt

/** Whole km/h, e.g. "24". */
fun formatSpeed(kmh: Double): String = kmh.coerceAtLeast(0.0).roundToInt().toString()

/** One decimal km/h, e.g. "18.4". */
fun formatSpeedDecimal(kmh: Double): String =
    String.format(Locale.US, "%.1f", kmh.coerceAtLeast(0.0))

/** Two decimals of km, e.g. "12.34". */
fun formatDistance(km: Double): String =
    String.format(Locale.US, "%.2f", km.coerceAtLeast(0.0))

/** Compact km for labels, e.g. "10" or "7.5". */
fun formatTargetKm(km: Double): String {
    return if (km % 1.0 == 0.0) km.roundToInt().toString()
    else String.format(Locale.US, "%.1f", km)
}

/** "M:SS" under an hour, otherwise "H:MM:SS". */
fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0)
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    else
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
