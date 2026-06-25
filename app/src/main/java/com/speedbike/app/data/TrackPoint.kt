package com.speedbike.app.data

/**
 * A single recorded GPS point of the ride. Kept free of any Android/osmdroid
 * types so the data layer stays independent of the map library.
 */
data class TrackPoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val timestamp: Long = 0L
)
