package com.speedbike.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

private val dateFormat = SimpleDateFormat("d MMM yyyy, HH:mm", Locale("uk"))

/** Human date for a ride, e.g. "25 черв 2026, 14:03". */
fun formatRideDate(epochMillis: Long): String = dateFormat.format(Date(epochMillis))
