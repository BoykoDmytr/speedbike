package com.speedbike.app.data.pet

/** A virtual cross-country journey driven by the rider's total real distance. */
object VirtualJourney {

    data class Leg(val from: String, val to: String, val km: Double)

    val legs: List<Leg> = listOf(
        Leg("Київ", "Житомир", 140.0),
        Leg("Житомир", "Рівне", 190.0),
        Leg("Рівне", "Львів", 210.0),
        Leg("Львів", "Краків", 330.0),
        Leg("Краків", "Прага", 530.0),
        Leg("Прага", "Відень", 290.0),
        Leg("Відень", "Будапешт", 240.0),
        Leg("Будапешт", "Загреб", 350.0),
        Leg("Загреб", "Венеція", 380.0),
        Leg("Венеція", "Рим", 530.0),
        Leg("Рим", "Барселона", 860.0),
        Leg("Барселона", "Париж", 830.0),
        Leg("Париж", "Амстердам", 430.0),
        Leg("Амстердам", "Берлін", 580.0),
        Leg("Берлін", "Варшава", 520.0),
        Leg("Варшава", "Київ", 690.0)
    )

    val totalRouteKm: Double = legs.sumOf { it.km }

    data class JourneyInfo(
        val from: String,
        val to: String,
        val legFraction: Float,
        val kmIntoLeg: Double,
        val legKm: Double,
        val completedLegs: Int,
        val totalKm: Double,
        val laps: Int
    )

    fun infoFor(totalKm: Double): JourneyInfo {
        val laps = (totalKm / totalRouteKm).toInt()
        var remaining = totalKm - laps * totalRouteKm
        var completed = laps * legs.size
        for (leg in legs) {
            if (remaining < leg.km) {
                return JourneyInfo(
                    from = leg.from,
                    to = leg.to,
                    legFraction = (remaining / leg.km).coerceIn(0.0, 1.0).toFloat(),
                    kmIntoLeg = remaining,
                    legKm = leg.km,
                    completedLegs = completed,
                    totalKm = totalKm,
                    laps = laps
                )
            }
            remaining -= leg.km
            completed++
        }
        // Exactly at the end of a lap.
        val last = legs.last()
        return JourneyInfo(last.from, last.to, 1f, last.km, last.km, completed, totalKm, laps)
    }
}
