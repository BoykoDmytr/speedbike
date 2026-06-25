package com.speedbike.app.util

/** Rough cycling calorie model based on MET values for the current speed. */
object Calories {

    /** MET (metabolic equivalent) for a cycling speed in km/h. */
    private fun metForSpeed(kmh: Double): Double = when {
        kmh < 1.0 -> 1.0      // basically resting
        kmh < 16.0 -> 4.0     // leisure
        kmh < 19.0 -> 6.0
        kmh < 22.0 -> 8.0
        kmh < 25.0 -> 10.0
        kmh < 30.0 -> 12.0
        else -> 15.0          // racing
    }

    /** kcal burned over [millis] at [speedKmh] for a rider of [weightKg]. */
    fun burned(speedKmh: Double, weightKg: Double, millis: Long): Double {
        val hours = millis / 3_600_000.0
        return metForSpeed(speedKmh) * weightKg * hours
    }
}
