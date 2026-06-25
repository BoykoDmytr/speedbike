package com.speedbike.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A completed ride, persisted to the local Room database. */
@Entity(tableName = "rides")
data class RideEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val endedAt: Long,
    val distanceMeters: Double,
    val durationMillis: Long,
    val avgSpeedKmh: Double,
    val maxSpeedKmh: Double,
    val elevationGainMeters: Double,
    val caloriesKcal: Double,
    /** Encoded "lat,lng,alt,ts;…" path — see [com.speedbike.app.data.PathCodec]. */
    val pathEncoded: String
) {
    val distanceKm: Double get() = distanceMeters / 1000.0
}
