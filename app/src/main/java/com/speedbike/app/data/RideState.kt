package com.speedbike.app.data

import kotlin.math.floor

/** Lifecycle of a ride. */
enum class RideStatus { IDLE, TRACKING, PAUSED }

/**
 * The full, immutable snapshot of the current ride. The UI renders directly
 * from this and the tracking service is the single writer.
 */
data class RideState(
    val status: RideStatus = RideStatus.IDLE,
    val currentSpeedKmh: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val distanceMeters: Double = 0.0,
    val durationMillis: Long = 0L,
    val elevationGainMeters: Double = 0.0,
    val caloriesKcal: Double = 0.0,
    val path: List<TrackPoint> = emptyList(),
    val lastPoint: TrackPoint? = null,
    val lastAltitude: Double? = null,

    // User settings (mirrored from SettingsStore)
    val targetDistanceKm: Double = 10.0,
    val alarmEnabled: Boolean = true,
    val repeatAlarm: Boolean = false,
    val useMiles: Boolean = false,
    val voiceEnabled: Boolean = false,
    val keepScreenOn: Boolean = true,
    val autoPause: Boolean = true,
    val weightKg: Double = 70.0,

    // Alarm / progress tracking
    val alarmsTriggered: Int = 0,
    val goalReached: Boolean = false,
    val isAlarmRinging: Boolean = false,
    val autoPaused: Boolean = false,
    val lastAnnouncedKm: Int = 0,
    val justFinished: Boolean = false
) {
    val distanceKm: Double get() = distanceMeters / 1000.0

    /** Average speed over the (moving) time accumulated by the service. */
    val avgSpeedKmh: Double
        get() {
            val hours = durationMillis / 3_600_000.0
            return if (hours > 0.0) distanceKm / hours else 0.0
        }

    /** The distance at which the alarm will ring next. */
    val nextThresholdKm: Double
        get() = if (targetDistanceKm > 0.0)
            targetDistanceKm * (alarmsTriggered + 1)
        else Double.POSITIVE_INFINITY

    /** The target shown on the gauge (grows with each interval when repeating). */
    val displayTargetKm: Double
        get() = if (repeatAlarm) nextThresholdKm else targetDistanceKm

    /** 0f..1f progress toward the current target / next interval. */
    val progress: Float
        get() {
            if (targetDistanceKm <= 0.0) return 0f
            // One-shot alarm: stay full once reached.
            if (!repeatAlarm && alarmsTriggered >= 1) return 1f
            val base = if (repeatAlarm) targetDistanceKm * alarmsTriggered else 0.0
            return ((distanceKm - base) / targetDistanceKm).coerceIn(0.0, 1.0).toFloat()
        }

    val wholeKm: Int get() = floor(distanceKm).toInt()

    val isActive: Boolean get() = status == RideStatus.TRACKING || status == RideStatus.PAUSED
}
