package com.speedbike.app.util

import java.util.Locale
import kotlin.math.roundToInt

/** Unit conversion + formatting. Internally everything is metric; this converts
 *  for display when the user prefers miles. */
object Units {
    private const val MILES_PER_KM = 0.621371
    private const val FEET_PER_METER = 3.28084

    fun distance(km: Double, miles: Boolean): Double = if (miles) km * MILES_PER_KM else km
    fun speed(kmh: Double, miles: Boolean): Double = if (miles) kmh * MILES_PER_KM else kmh
    fun height(meters: Double, miles: Boolean): Double = if (miles) meters * FEET_PER_METER else meters

    fun distUnit(miles: Boolean): String = if (miles) "ми" else "км"
    fun speedUnit(miles: Boolean): String = if (miles) "mi/h" else "км/год"
    fun heightUnit(miles: Boolean): String = if (miles) "фт" else "м"

    /** Whole speed, e.g. "24". */
    fun fmtSpeed(kmh: Double, miles: Boolean): String =
        speed(kmh, miles).coerceAtLeast(0.0).roundToInt().toString()

    /** One-decimal speed, e.g. "18.4". */
    fun fmtSpeed1(kmh: Double, miles: Boolean): String =
        String.format(Locale.US, "%.1f", speed(kmh, miles).coerceAtLeast(0.0))

    /** Two-decimal distance, e.g. "12.34". */
    fun fmtDistance(km: Double, miles: Boolean): String =
        String.format(Locale.US, "%.2f", distance(km, miles).coerceAtLeast(0.0))

    /** Compact distance for labels, e.g. "10" or "7.5". */
    fun fmtCompact(km: Double, miles: Boolean): String {
        val v = distance(km, miles)
        return if (v % 1.0 == 0.0) v.roundToInt().toString()
        else String.format(Locale.US, "%.1f", v)
    }

    /** Whole height, e.g. "143". */
    fun fmtHeight(meters: Double, miles: Boolean): String =
        height(meters, miles).coerceAtLeast(0.0).roundToInt().toString()
}
