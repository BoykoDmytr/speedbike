package com.speedbike.app.data

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
    val path: List<TrackPoint> = emptyList(),
    val lastPoint: TrackPoint? = null,
    // User settings
    val targetDistanceKm: Double = 10.0,
    val alarmEnabled: Boolean = true,
    // Goal / alarm flags
    val goalReached: Boolean = false,
    val isAlarmRinging: Boolean = false
) {
    val distanceKm: Double get() = distanceMeters / 1000.0

    /** Overall average speed over the moving time accumulated by the service. */
    val avgSpeedKmh: Double
        get() {
            val hours = durationMillis / 3_600_000.0
            return if (hours > 0.0) distanceKm / hours else 0.0
        }

    /** 0f..1f progress toward the configured target distance. */
    val progress: Float
        get() = if (targetDistanceKm > 0.0)
            (distanceKm / targetDistanceKm).coerceIn(0.0, 1.0).toFloat()
        else 0f

    val isActive: Boolean get() = status == RideStatus.TRACKING || status == RideStatus.PAUSED
}
