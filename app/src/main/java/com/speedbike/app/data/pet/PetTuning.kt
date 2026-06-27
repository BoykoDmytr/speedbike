package com.speedbike.app.data.pet

import java.util.TimeZone
import kotlin.math.max

/** All balancing constants and derived formulas for the pet game in one place. */
object PetTuning {

    // Rewards per ride
    const val XP_PER_KM = 10.0
    const val XP_PER_ELEV_M = 0.2
    const val XP_GOAL_BONUS = 50.0
    const val COINS_PER_KM = 5.0
    const val COINS_GOAL_BONUS = 20.0

    // Energy / decay (per hour while not riding)
    const val ENERGY_PER_KM = 8.0
    const val ENERGY_DECAY_PER_HOUR = 0.6
    const val HAPPINESS_DECAY_PER_HOUR = 0.4
    const val RUST_PER_HOUR = 0.5
    const val FITNESS_DECAY_PER_HOUR = 0.2
    const val LOW_ENERGY = 30f

    // Per-ride boosts
    const val HAPPY_PER_RIDE = 8.0
    const val FITNESS_PER_KM = 0.5
    const val STRENGTH_PER_ELEV_M = 0.05
    const val RUST_CLEARED_PER_KM = 2.0

    fun xpForLevel(level: Int): Long = 50L * (level - 1).coerceAtLeast(0) * level
    fun levelForXp(xp: Long): Int {
        var l = 1
        while (xpForLevel(l + 1) <= xp) l++
        return l
    }

    fun stageFor(totalKm: Double): PetStage = when {
        totalKm < 1.0 -> PetStage.EGG
        totalKm < 25.0 -> PetStage.BABY
        totalKm < 100.0 -> PetStage.TEEN
        totalKm < 500.0 -> PetStage.ADULT
        else -> PetStage.CHAMPION
    }

    fun styleFor(totalKm: Double, totalElevM: Double, totalMovingMillis: Long): PetStyle {
        if (totalKm < 15.0) return PetStyle.NONE
        val climbPerKm = if (totalKm > 0) totalElevM / totalKm else 0.0
        val hours = totalMovingMillis / 3_600_000.0
        val avg = if (hours > 0) totalKm / hours else 0.0
        return when {
            climbPerKm >= 12.0 -> PetStyle.CLIMBER
            avg >= 21.0 -> PetStyle.SPRINTER
            totalKm >= 80.0 -> PetStyle.MARATHONER
            else -> PetStyle.ALLROUNDER
        }
    }

    fun moodFor(energy: Float, happiness: Float, rust: Float, dailyGoalMet: Boolean): PetMood = when {
        energy < 15f -> PetMood.SLEEPY
        energy < 35f || rust > 50f -> PetMood.HUNGRY
        happiness < 35f -> PetMood.SAD
        dailyGoalMet && happiness > 75f -> PetMood.EXCITED
        happiness > 60f -> PetMood.HAPPY
        else -> PetMood.CONTENT
    }

    /** Local-time day index, stable across all API levels (no java.time needed). */
    fun epochDay(nowMillis: Long): Long {
        val offset = TimeZone.getDefault().getOffset(nowMillis)
        return (nowMillis + offset) / 86_400_000L
    }

    fun fitnessIntensity(avgSpeedKmh: Double): Double =
        max(0.3, (avgSpeedKmh / 20.0)).coerceAtMost(2.0)
}
