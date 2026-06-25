package com.speedbike.app.data

/**
 * Compact, dependency-free encoding of a ride path for DB storage.
 * Format: "lat,lng,alt,ts;lat,lng,alt,ts;…".
 */
object PathCodec {

    fun encode(points: List<TrackPoint>): String =
        points.joinToString(";") { "${it.latitude},${it.longitude},${it.altitude},${it.timestamp}" }

    fun decode(encoded: String): List<TrackPoint> {
        if (encoded.isBlank()) return emptyList()
        return encoded.split(";").mapNotNull { chunk ->
            val parts = chunk.split(",")
            if (parts.size < 2) return@mapNotNull null
            val lat = parts[0].toDoubleOrNull() ?: return@mapNotNull null
            val lng = parts[1].toDoubleOrNull() ?: return@mapNotNull null
            val alt = parts.getOrNull(2)?.toDoubleOrNull() ?: 0.0
            val ts = parts.getOrNull(3)?.toLongOrNull() ?: 0L
            TrackPoint(lat, lng, alt, ts)
        }
    }
}
