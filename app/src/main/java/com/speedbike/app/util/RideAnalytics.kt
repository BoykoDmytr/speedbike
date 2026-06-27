package com.speedbike.app.util

import com.speedbike.app.data.TrackPoint
import com.speedbike.app.data.db.RideEntity
import java.util.Calendar
import java.util.TimeZone

/** Derived analytics computed from a ride path or the whole ride history. */
object RideAnalytics {

    data class Series(
        val distancesKm: List<Float>,
        val speedsKmh: List<Float>,
        val altitudesM: List<Float>
    ) {
        val hasSpeed: Boolean get() = speedsKmh.any { it > 0f }
        val hasAltitude: Boolean get() = altitudesM.any { it != 0f }
    }

    data class Split(val km: Int, val seconds: Long, val speedKmh: Double)

    data class Records(
        val totalRides: Int = 0,
        val totalKm: Double = 0.0,
        val totalElevationM: Double = 0.0,
        val totalCalories: Double = 0.0,
        val longestRideKm: Double = 0.0,
        val biggestClimbM: Double = 0.0,
        val topSpeedKmh: Double = 0.0,
        val fastest5kSeconds: Long? = null
    )

    private fun distM(a: TrackPoint, b: TrackPoint): Double {
        // Haversine
        val r = 6_371_000.0
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val la1 = Math.toRadians(a.latitude)
        val la2 = Math.toRadians(b.latitude)
        val h = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(la1) * Math.cos(la2) * Math.sin(dLon / 2) * Math.sin(dLon / 2)
        return 2 * r * Math.asin(Math.min(1.0, Math.sqrt(h)))
    }

    /** Speed (smoothed) and altitude vs cumulative distance. */
    fun series(path: List<TrackPoint>): Series {
        if (path.size < 2) return Series(emptyList(), emptyList(), emptyList())
        val dist = ArrayList<Float>(path.size)
        val speed = ArrayList<Float>(path.size)
        val alt = ArrayList<Float>(path.size)
        var cum = 0.0
        dist.add(0f); speed.add(0f); alt.add(path[0].altitude.toFloat())
        for (i in 1 until path.size) {
            val d = distM(path[i - 1], path[i])
            cum += d
            val dt = (path[i].timestamp - path[i - 1].timestamp) / 1000.0
            val s = if (dt > 0) (d / dt) * 3.6 else 0.0
            dist.add((cum / 1000.0).toFloat())
            speed.add(s.coerceIn(0.0, 120.0).toFloat())
            alt.add(path[i].altitude.toFloat())
        }
        // light smoothing of speed
        val sm = ArrayList<Float>(speed.size)
        for (i in speed.indices) {
            val a = speed[(i - 1).coerceAtLeast(0)]
            val b = speed[i]
            val c = speed[(i + 1).coerceAtMost(speed.size - 1)]
            sm.add((a + b + c) / 3f)
        }
        return Series(dist, sm, alt)
    }

    /** Per-kilometre splits (time + average speed for each completed km). */
    fun splits(path: List<TrackPoint>): List<Split> {
        if (path.size < 2) return emptyList()
        val result = ArrayList<Split>()
        var cum = 0.0
        var kmMark = 1
        var lastBoundaryTime = path.first().timestamp
        for (i in 1 until path.size) {
            cum += distM(path[i - 1], path[i])
            while (cum >= kmMark * 1000.0) {
                val t = path[i].timestamp
                val sec = (t - lastBoundaryTime) / 1000
                val speed = if (sec > 0) 3600.0 / sec else 0.0
                result.add(Split(kmMark, sec, speed))
                lastBoundaryTime = t
                kmMark++
            }
        }
        return result
    }

    private fun fastestWindowSeconds(path: List<TrackPoint>, windowM: Double): Long? {
        if (path.size < 2) return null
        // cumulative distance + time
        val n = path.size
        val cum = DoubleArray(n)
        for (i in 1 until n) cum[i] = cum[i - 1] + distM(path[i - 1], path[i])
        if (cum[n - 1] < windowM) return null
        var best = Long.MAX_VALUE
        var j = 0
        for (i in 0 until n) {
            while (j < n && cum[j] - cum[i] < windowM) j++
            if (j >= n) break
            val sec = (path[j].timestamp - path[i].timestamp) / 1000
            if (sec in 1L until best) best = sec
        }
        return if (best == Long.MAX_VALUE) null else best
    }

    fun records(rides: List<RideEntity>, decode: (String) -> List<TrackPoint>): Records {
        if (rides.isEmpty()) return Records()
        var fastest5k: Long? = null
        for (ride in rides) {
            if (ride.distanceKm >= 5.0) {
                val w = fastestWindowSeconds(decode(ride.pathEncoded), 5000.0)
                if (w != null && (fastest5k == null || w < fastest5k!!)) fastest5k = w
            }
        }
        return Records(
            totalRides = rides.size,
            totalKm = rides.sumOf { it.distanceKm },
            totalElevationM = rides.sumOf { it.elevationGainMeters },
            totalCalories = rides.sumOf { it.caloriesKcal },
            longestRideKm = rides.maxOf { it.distanceKm },
            biggestClimbM = rides.maxOf { it.elevationGainMeters },
            topSpeedKmh = rides.maxOf { it.maxSpeedKmh },
            fastest5kSeconds = fastest5k
        )
    }

    /** Sum of ride distances (km) since the start of the current week / month. */
    fun distanceSince(rides: List<RideEntity>, startMillis: Long): Double =
        rides.filter { it.startedAt >= startMillis }.sumOf { it.distanceKm }

    fun startOfDay(now: Long): Long {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.timeInMillis = now
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun startOfWeek(now: Long): Long {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.timeInMillis = now
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun startOfMonth(now: Long): Long {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.timeInMillis = now
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
